package me.qyh.blog.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import me.qyh.blog.dao.ArticleDao;
import me.qyh.blog.entity.Article;
import me.qyh.blog.lock.LockProtected;

@Component
public class ArticleCache {

	private static final String CACHE_NAME = "articleCache";

	@Autowired
	private ArticleDao articleDao;
	@Autowired
	private CacheManager cacheManager;

	@LockProtected
	@Cacheable(value = CACHE_NAME, key = "'article-'+#id", unless = "#result == null || !#result.isPublished()")
	public Article getArticleWithLockCheck(Integer id) {
		return articleDao.selectById(id);
	}

	@LockProtected
	@Cacheable(value = CACHE_NAME, key = "'article-'+#alias", unless = "#result == null || !#result.isPublished()")
	public Article getArticleWithLockCheck(String alias) {
		return articleDao.selectByAlias(alias);
	}

	@Cacheable(value = CACHE_NAME, key = "'article-'+#id", unless = "#result == null || !#result.isPublished()")
	public Article getArticle(Integer id) {
		return articleDao.selectById(id);
	}

	public void evit(Article article) {
		Cache cache = cacheManager.getCache(CACHE_NAME);
		if (cache != null) {
			if (article.getAlias() != null)
				cache.evict("article-" + article.getAlias());
			cache.evict("article-" + article.getId());
		}
	}
}
