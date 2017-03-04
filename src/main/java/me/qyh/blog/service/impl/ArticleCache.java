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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;

import me.qyh.blog.dao.ArticleDao;
import me.qyh.blog.entity.Article;

@Component
public class ArticleCache {

	@Autowired
	private ArticleDao articleDao;
	@Autowired
	private PlatformTransactionManager platformTransactionManager;

	public Article getArticle(String alias) {
		Integer id = aliasCache.get(alias);
		if (id != null) {
			return getArticle(id);
		}
		return null;
	}

	public Article getArticle(Integer id) {
		Article article = idCache.get(id);
		if (article != null) {
			return new Article(article);
		}
		return null;
	}

	public synchronized void evit(Article article) {
		Article art = idCache.getIfPresent(article.getId());
		if (art != null) {
			String alias = art.getAlias();
			if (alias != null) {
				aliasCache.invalidate(alias);
			}
			idCache.invalidate(article.getId());
		}
	}

	private final LoadingCache<String, Integer> aliasCache = Caffeine.newBuilder()
			.build(new CacheLoader<String, Integer>() {

				@Override
				public Integer load(String key) throws Exception {
					DefaultTransactionDefinition dtd = new DefaultTransactionDefinition();
					dtd.setReadOnly(true);
					TransactionStatus status = platformTransactionManager.getTransaction(dtd);
					try {
						Article article = articleDao.selectByAlias(key);
						if (article != null && article.isPublished()) {
							idCache.put(article.getId(), article);
							return article.getId();
						}
					} catch (RuntimeException | Error e) {
						status.setRollbackOnly();
						throw e;
					} finally {
						platformTransactionManager.commit(status);
					}
					return null;
				}
			});

	private final LoadingCache<Integer, Article> idCache = Caffeine.newBuilder()
			.build(new CacheLoader<Integer, Article>() {

				@Override
				public Article load(Integer key) throws Exception {
					DefaultTransactionDefinition dtd = new DefaultTransactionDefinition();
					dtd.setReadOnly(true);
					TransactionStatus status = platformTransactionManager.getTransaction(dtd);
					try {
						Article article = articleDao.selectById(key);
						if (article != null && article.isPublished()) {
							String alias = article.getAlias();
							if (alias != null) {
								aliasCache.put(article.getAlias(), article.getId());
							}
							return article;
						}
						return null;
					} catch (RuntimeException | Error e) {
						status.setRollbackOnly();
						throw e;
					} finally {
						platformTransactionManager.commit(status);
					}
				}
			});
}
