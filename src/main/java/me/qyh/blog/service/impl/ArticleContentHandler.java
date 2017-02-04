package me.qyh.blog.service.impl;

import me.qyh.blog.entity.Article;

/**
 * 文章内容处理器，用于文章内容的调整,<b>同时也将用于构建索引时文章内容的预处理</b>
 * <p>
 * 可以为空
 * </p>
 * 
 * @see NRTArticleIndexer
 * @see ArticleServiceImpl
 * 
 * @author Administrator
 *
 */
public interface ArticleContentHandler {
	/**
	 * 用来处理文章
	 * 
	 * @param article
	 */
	void handle(Article article);

	/**
	 * 用来处理预览文章
	 * 
	 * @param article
	 */
	void handlePreview(Article article);
}