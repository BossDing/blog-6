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
package me.qyh.blog.service.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.locks.StampedLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.transaction.event.TransactionalEventListener;

import me.qyh.blog.config.Constants;
import me.qyh.blog.entity.Article;
import me.qyh.blog.evt.ArticleEvent;
import me.qyh.blog.exception.SystemException;
import me.qyh.blog.service.impl.ArticleServiceImpl.ArticleViewedLogger;
import me.qyh.blog.util.FileUtils;
import me.qyh.blog.util.SerializationUtils;

/**
 * 将最近访问的文章纪录在内存中
 * <p>
 * <b>可能文章状态可能会变更，所以实际返回的数量可能小于<i>max</i></b>
 * </p>
 * <p>
 * </p>
 * 
 * @author Administrator
 *
 */
public class SyncArticleViewdLogger implements InitializingBean, ArticleViewedLogger {

	private static final Logger LOGGER = LoggerFactory.getLogger(SyncArticleViewdLogger.class);

	private final int max;
	private Map<Integer, Article> articles;
	private StampedLock lock = new StampedLock();

	/**
	 * 应用关闭时当前访问的文章存入文件中
	 */
	private final Path sdfile = Constants.DAT_DIR.resolve("sync_articles_viewd.dat");

	private long timestamp = Long.MAX_VALUE;

	public SyncArticleViewdLogger(int max) {
		if (max < 0) {
			throw new SystemException("max必须大于0");
		}
		this.max = max;

		articles = new LinkedHashMap<Integer, Article>() {

			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			protected boolean removeEldestEntry(Entry<Integer, Article> eldest) {
				return size() > max;
			}

		};
	}

	public SyncArticleViewdLogger() {
		this(10);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>
	 * <b>存在'延迟'，一些文章的更新操作不会被立即体现</b>
	 * </p>
	 */
	@Override
	public List<Article> getViewdArticles(int num) {
		long stamp = lock.tryOptimisticRead();
		List<Article> result = getCurrentViewed(num);
		if (!lock.validate(stamp)) {
			stamp = lock.readLock();
			try {
				result = getCurrentViewed(num);
			} finally {
				lock.unlockRead(stamp);
			}
		}
		return result;
	}

	private List<Article> getCurrentViewed(int num) {
		List<Article> result = new ArrayList<>(articles.values());
		if (!result.isEmpty()) {
			Collections.reverse(result);
			int finalNum = Math.min(num, max);
			if (result.size() > finalNum) {
				result = result.subList(0, finalNum - 1);
			}
		}
		return result;
	}

	@Override
	public void logViewd(Article article) {
		long stamp = lock.writeLock();
		try {
			if (timestamp <= System.currentTimeMillis()) {
				return;
			}
			articles.remove(article.getId());
			articles.put(article.getId(), article);
		} finally {
			lock.unlockWrite(stamp);
		}
	}

	@TransactionalEventListener
	public void handleArticleEvent(ArticleEvent evt) {
		long stamp = lock.writeLock();
		// 设置当前时间
		timestamp = System.currentTimeMillis();
		try {
			switch (evt.getEventType()) {
			case DELETE:
				evt.getArticles().forEach(art -> articles.remove(art.getId()));
				break;
			case UPDATE:
				for (Article art : evt.getArticles()) {
					boolean valid = art.isPublished() && !art.isPrivate();
					if (!valid) {
						articles.remove(art.getId());
					} else {
						articles.replace(art.getId(), new Article(art));
					}
				}
				break;
			default:
				break;
			}
		} finally {
			timestamp = Long.MAX_VALUE;
			lock.unlockWrite(stamp);
		}
	}

	@EventListener
	public void handleContextCloseEvent(ContextClosedEvent evt) throws IOException {
		if (!articles.isEmpty()) {
			SerializationUtils.serialize(new LinkedHashMap<>(articles), sdfile);
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (Files.exists(sdfile)) {
			try {
				this.articles.putAll(SerializationUtils.deserialize(sdfile));
			} catch (Exception e) {
				LOGGER.warn("反序列化文件" + sdfile + "失败：" + e.getMessage(), e);
			} finally {
				if (!FileUtils.deleteQuietly(sdfile)) {
					LOGGER.warn("删除文件" + sdfile + "失败");
				}
			}
		}
	}
}