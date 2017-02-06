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

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import com.google.common.base.Stopwatch;

import me.qyh.blog.dao.ArticleDao;
import me.qyh.blog.entity.Article;
import me.qyh.blog.evt.ArticleIndexRebuildEvent;

@Component
public class ArticleIndexRebuilder implements ApplicationListener<ArticleIndexRebuildEvent> {

	@Autowired
	private ArticleDao articleDao;
	@Autowired
	private PlatformTransactionManager platformTransactionManager;
	@Autowired
	private NRTArticleIndexer articleIndexer;
	private static final Logger LOGGER = LoggerFactory.getLogger(ArticleIndexRebuilder.class);

	@Override
	@Async
	public void onApplicationEvent(ArticleIndexRebuildEvent event) {
		DefaultTransactionDefinition dtd = new DefaultTransactionDefinition();
		dtd.setReadOnly(true);
		TransactionStatus status = platformTransactionManager.getTransaction(dtd);
		try {
			Stopwatch sw = Stopwatch.createStarted();
			articleIndexer.deleteAll();
			List<Article> articles = articleDao.selectPublished(null);
			for (Article article : articles) {
				articleIndexer.addOrUpdateDocument(article);
			}
			sw.stop();
			LOGGER.debug("重建索引花费了：" + sw.elapsed(TimeUnit.MILLISECONDS) + "ms");
		} catch (RuntimeException | Error e) {
			status.setRollbackOnly();
			LOGGER.error("重建索引过程中发生异常：" + e.getMessage(), e);
		} finally {
			platformTransactionManager.commit(status);
		}
	}
}
