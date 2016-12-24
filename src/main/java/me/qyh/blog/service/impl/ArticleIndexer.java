package me.qyh.blog.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import me.qyh.blog.dao.ArticleDao;
import me.qyh.blog.entity.Article;

@Component
public class ArticleIndexer {

	@Autowired
	private NRTArticleIndexer articleIndexer;
	@Autowired
	private ArticleDao articleDao;

	@Transactional(readOnly = true)
	public synchronized void rebuildIndex() {
		articleIndexer.deleteAll();
		List<Article> articles = articleDao.selectPublished(null);
		for (Article article : articles) {
			articleIndexer.addOrUpdateDocument(article);
		}
	}

	public NRTArticleIndexer getIndexer() {
		return articleIndexer;
	}
}
