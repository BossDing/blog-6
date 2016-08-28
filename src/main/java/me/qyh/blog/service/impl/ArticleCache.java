package me.qyh.blog.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import me.qyh.blog.dao.ArticleDao;
import me.qyh.blog.entity.Article;
import me.qyh.blog.lock.LockProtected;

/**
 * 为了解决缓存内部调用失效
 * 
 */
@Component
public class ArticleCache {

	@Autowired
	private ArticleDao articleDao;

	@Transactional(readOnly = true)
	@LockProtected
	@Cacheable(value = "articleCache", key = "'article-'+#id", unless = "#result == null || !#result.isCacheable()")
	public Article getArticle(Integer id) {
		return articleDao.selectById(id);
	}

}
