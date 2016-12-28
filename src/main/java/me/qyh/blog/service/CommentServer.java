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
package me.qyh.blog.service;

import java.util.List;
import java.util.Map;

import me.qyh.blog.entity.Article;
import me.qyh.blog.entity.Space;

public interface CommentServer {

	/**
	 * 查询文章列表评论数
	 * 
	 * @param ids
	 *            文章ids
	 * @return 文章id和评论数的map
	 */
	Map<Integer, Integer> queryArticlesCommentCount(List<Integer> ids);

	/**
	 * 查询文章评论数
	 * 
	 * @param id
	 * @return
	 */
	int queryArticleCommentCount(Integer id);

	/**
	 * 查询总的文章评论数
	 * 
	 * @param space
	 *            空间，如果为空则查询全部
	 * @param queryPrivate
	 *            是否查询私人博客
	 * @return
	 */
	int queryArticlesTotalCommentCount(Space space, boolean queryPrivate);

	/**
	 * 根据文章删除评论
	 * 
	 * @param article
	 *            文章
	 */
	void deleteComments(Article article);

}
