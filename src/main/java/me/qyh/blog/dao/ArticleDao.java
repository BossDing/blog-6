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
package me.qyh.blog.dao;

import java.sql.Timestamp;
import java.util.List;

import org.apache.ibatis.annotations.Param;

import me.qyh.blog.bean.ArticleDateFile;
import me.qyh.blog.bean.ArticleDateFiles.ArticleDateFileMode;
import me.qyh.blog.bean.ArticleSpaceFile;
import me.qyh.blog.bean.ArticleStatistics;
import me.qyh.blog.entity.Article;
import me.qyh.blog.entity.Space;
import me.qyh.blog.pageparam.ArticleQueryParam;

public interface ArticleDao {

	Article selectById(int id);

	List<ArticleDateFile> selectDateFiles(@Param("space") Space space, @Param("mode") ArticleDateFileMode mode,
			@Param("queryPrivate") boolean queryPrivate);

	List<ArticleSpaceFile> selectSpaceFiles(@Param("queryPrivate") boolean queryPrivate);

	List<Article> selectScheduled(Timestamp date);

	void update(Article article);

	int selectCount(ArticleQueryParam param);

	List<Article> selectPage(ArticleQueryParam param);

	void insert(Article article);

	List<Article> selectPublished(@Param("space") Space space);

	List<Article> selectByIds(List<Integer> ids);

	void deleteById(Integer id);

	void updateHits(@Param("id") Integer id, @Param("hits") int increase);

	void updateComments(@Param("id") Integer id, @Param("comments") int increase);

	List<Article> selectAll();

	Article getPreviousArticle(@Param("article") Article article, @Param("queryPrivate") boolean queryPrivate);

	Article getNextArticle(@Param("article") Article article, @Param("queryPrivate") boolean queryPrivate);

	ArticleStatistics selectStatistics(@Param("space") Space space, @Param("queryPrivate") boolean queryPrivate,
			@Param("queryHidden") boolean queryHidden);

	Article selectByAlias(String alias);

	List<Article> selectRecentArticles(int limit);

}
