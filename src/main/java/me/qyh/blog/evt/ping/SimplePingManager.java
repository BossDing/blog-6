/*
 * Copyright 2016 qyh.me
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package me.qyh.blog.evt.ping;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.util.CollectionUtils;

import com.google.common.collect.Lists;

import me.qyh.blog.entity.Article;
import me.qyh.blog.evt.ArticleEvent;
import me.qyh.blog.evt.ArticleEvent.EventType;
import me.qyh.blog.exception.SystemException;

/**
 * 简单的ping管理器
 * 
 * @author Administrator
 *
 */
public class SimplePingManager implements ApplicationListener<ArticleEvent>, InitializingBean {

	protected static final Logger LOGGER = LoggerFactory.getLogger(SimplePingManager.class);

	private List<PingService> pingServices = Lists.newArrayList();
	private final String blogName;

	private int awaitSeconds;

	private ExecutorService executor;

	public SimplePingManager(String blogName) {
		super();
		this.blogName = blogName;
	}

	@Async
	@Override
	public void onApplicationEvent(ArticleEvent event) {
		List<Article> articles = event.getArticles();
		articles.stream().filter(article -> needPing(event.getEventType(), article)).forEach(this::ping);
	}

	private boolean needPing(EventType eventType, Article article) {
		return ((EventType.INSERT.equals(eventType) || EventType.UPDATE.equals(eventType)) && article.isPublished()
				&& !article.hasLock() && !article.isPrivate());

	}

	private void ping(Article article) {
		pingServices.stream().forEach(pingService -> CompletableFuture.supplyAsync(() -> {
			try {
				pingService.ping(article, blogName);
			} catch (Exception e) {
				LOGGER.error(e.getMessage(), e);
			}
			return null;
		}, executor));
	}

	public void setPingServices(List<PingService> pingServices) {
		this.pingServices = pingServices;
	}

	public void close() {
		executor.shutdown();
		try {
			executor.awaitTermination(awaitSeconds, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (CollectionUtils.isEmpty(pingServices)) {
			throw new SystemException("ping服务不能为空");
		}
		if (awaitSeconds <= 0) {
			awaitSeconds = 60;
		}
		executor = Executors.newFixedThreadPool(pingServices.size());
	}

	public void setAwaitSeconds(int awaitSeconds) {
		this.awaitSeconds = awaitSeconds;
	}

}
