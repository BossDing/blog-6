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
	PageResult<Article> query(ArticleQueryParam param, ArticlesDetailQuery query);

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

	public interface ArticlesDetailQuery {

		List<Article> queryArticle(List<Integer> articleIds);
	}

}
