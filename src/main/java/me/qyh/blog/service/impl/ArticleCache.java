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
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import me.qyh.blog.dao.ArticleDao;
import me.qyh.blog.entity.Article;

@Component
public class ArticleCache {

	private static final String CACHE_NAME = "articleCache";

	@Autowired
	private ArticleDao articleDao;
	@Autowired
	private CacheManager cacheManager;

	@Cacheable(value = CACHE_NAME, key = "'article-'+#alias", unless = "#result == null || !#result.isPublished()")
	@Transactional(readOnly = true)
	public Article getArticle(String alias) {
		return articleDao.selectByAlias(alias);
	}

	@Cacheable(value = CACHE_NAME, key = "'article-'+#id", unless = "#result == null || !#result.isPublished()")
	public Article getArticle(Integer id) {
		return articleDao.selectById(id);
	}

	public void evit(Article article) {
		Cache cache = cacheManager.getCache(CACHE_NAME);
		if (cache != null) {
			if (article.getAlias() != null) {
				cache.evict("article-" + article.getAlias());
			}
			cache.evict("article-" + article.getId());
		}
	}
}
