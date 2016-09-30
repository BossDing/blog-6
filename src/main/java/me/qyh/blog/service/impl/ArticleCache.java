package me.qyh.blog.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import me.qyh.blog.dao.ArticleDao;
import me.qyh.blog.entity.Article;
import me.qyh.blog.lock.LockProtected;

@Component
public class ArticleCache {

	@Autowired
	private ArticleDao articleDao;

	@LockProtected
	@Cacheable(value = "articleCache", key = "'article-'+#id", unless = "#result == null || !#result.isPublished()")
	public Article getArticleWithLockCheck(Integer id) {
		return articleDao.selectById(id);
	}

	@LockProtected
	@Cacheable(value = "articleCache", key = "'article-'+#alias", unless = "#result == null || !#result.isPublished()")
	public Article getArticleWithLockCheck(String alias) {
		return articleDao.selectByAlias(alias);
	}

	@Cacheable(value = "articleCache", key = "'article-'+#id", unless = "#result == null || !#result.isPublished()")
	public Article getArticle(Integer id) {
		return articleDao.selectById(id);
	}

}
