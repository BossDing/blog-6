package me.qyh.blog.service.impl;

import java.util.List;

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
	 * 提取博客摘要
	 * 
	 * @param content
	 * @param max
	 *            摘要最大长度
	 * @return
	 */
	String getSummary(String content, int max);

	/**
	 * 提取博客标签
	 * 
	 * @param content
	 * @param max
	 *            标签最大数量
	 * @return
	 */
	List<String> getTags(String content, int max);

	/**
	 * 删除所有索引文档
	 */
	void deleteAll();

	/**
	 * 新增词库
	 * 
	 * @param tags
	 */
	void addTags(String... tags);

	/**
	 * 删除词
	 * 
	 * @param tags
	 */
	void removeTag(String... tags);

}
