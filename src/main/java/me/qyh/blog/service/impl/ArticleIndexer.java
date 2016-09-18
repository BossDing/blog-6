package me.qyh.blog.service.impl;

import me.qyh.blog.entity.Article;
import me.qyh.blog.pageparam.ArticleQueryParam;
import me.qyh.blog.pageparam.PageResult;

public interface ArticleIndexer {

	/**
	 * 添加博客索引
	 * 
	 * @param article
	 */
	void addOrUpdateDocument(Article article);

	/**
	 * 删除博客索引
	 * 
	 * @param id
	 */
	void deleteDocument(Integer id);

	/**
	 * 查询博客
	 * 
	 * @param param
	 * @return
	 */
	PageResult<Integer> query(ArticleQueryParam param);

	/**
	 * 删除所有索引文档
	 */
	void deleteAll();

	/**
	 * 新增标签
	 * 
	 * @param tags
	 */
	void addTags(String... tags);

	/**
	 * 删除标签
	 * 
	 * @param tags
	 */
	void removeTag(String... tags);

	/**
	 * 重载标签库
	 */
	void reloadTags();

}
