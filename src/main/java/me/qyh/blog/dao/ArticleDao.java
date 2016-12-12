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

/**
 * 
 * @author Administrator
 *
 */
public interface ArticleDao {

	/**
	 * 根据id查询文章
	 * 
	 * @param id
	 * @return 文章，如果id对应的文章不存在，返回null
	 */
	Article selectById(int id);

	/**
	 * 查询文章日期归档
	 * 
	 * @param space
	 *            空间
	 * @param mode
	 *            日期归档模式
	 * @param queryPrivate
	 *            是否查询私人文章
	 * @return 文章日期归档集合
	 */
	List<ArticleDateFile> selectDateFiles(@Param("space") Space space, @Param("mode") ArticleDateFileMode mode,
			@Param("queryPrivate") boolean queryPrivate);

	/**
	 * 查询文章空间归档
	 * 
	 * @param queryPrivate
	 *            是否查询私有文章
	 * @return 文章空间归档集合
	 */
	List<ArticleSpaceFile> selectSpaceFiles(@Param("queryPrivate") boolean queryPrivate);

	/**
	 * 查询截至日期前的待发布文章
	 * 
	 * @param date
	 *            截止日期
	 * @return
	 */
	List<Article> selectScheduled(Timestamp date);

	/**
	 * 更新文章
	 * 
	 * @param article
	 *            待更新的文章
	 */
	void update(Article article);

	/**
	 * 查询文章数量
	 * 
	 * @param param
	 *            查询参数
	 * @return 文章数量
	 */
	int selectCount(ArticleQueryParam param);

	/**
	 * 查询文章列表
	 * 
	 * @param param
	 *            查询参数
	 * @return 文章列表
	 */
	List<Article> selectPage(ArticleQueryParam param);

	/**
	 * 插入文章
	 * 
	 * @param article
	 *            待插入的文章
	 */
	void insert(Article article);

	/**
	 * 查询某个空间下的所有发布的文章
	 * 
	 * @param space
	 *            空间，如果为空，查询全部发布的文章
	 * @return 文章集合
	 */
	List<Article> selectPublished(@Param("space") Space space);

	/**
	 * 根据指定id集合查询对应的文章
	 * 
	 * @param ids
	 *            文章id集合
	 * @return id集合对应的文章集合
	 */
	List<Article> selectByIds(List<Integer> ids);

	/**
	 * 根据文章id删除文章
	 * 
	 * @param id
	 *            文章id
	 */
	void deleteById(Integer id);

	/**
	 * 更新文章的点击量
	 * 
	 * @param id
	 *            文章的id
	 * @param increase
	 *            <strong>增加的</strong>点击量
	 */
	void updateHits(@Param("id") Integer id, @Param("hits") int increase);

	/**
	 * 上一篇文章
	 * 
	 * @param article
	 *            当前文章
	 * @param queryPrivate
	 *            是否查询私有文章
	 * @return 上一篇，如果不存在，返回null
	 */
	Article getPreviousArticle(@Param("article") Article article, @Param("queryPrivate") boolean queryPrivate);

	/**
	 * 下一篇文章
	 * 
	 * @param article
	 *            当前文章
	 * @param queryPrivate
	 *            是否查询私有文章
	 * @return 下一篇文章，如果不存在，返回null
	 */
	Article getNextArticle(@Param("article") Article article, @Param("queryPrivate") boolean queryPrivate);

	/**
	 * 查询文章统计
	 * 
	 * @param space
	 *            空间，如果为空，则查询全部
	 * @param queryPrivate
	 *            是否查询私人文章
	 * @param queryHidden
	 *            是否查询隐藏的文章
	 * @return 文章统计
	 */
	ArticleStatistics selectStatistics(@Param("space") Space space, @Param("queryPrivate") boolean queryPrivate,
			@Param("queryHidden") boolean queryHidden);

	/**
	 * 根据文章的别名查询文章
	 * 
	 * @param alias
	 *            文章别名
	 * @return 文章，如果不存在，返回null
	 */
	Article selectByAlias(String alias);

	/**
	 * 查询最近的文章
	 * 
	 * @param limit
	 *            最大返回条数
	 * @return 最近的文章集合
	 */
	List<Article> selectRecentArticles(int limit);

	/**
	 * 删除锁
	 * 
	 * @param lockId
	 *            锁id
	 */
	void deleteLock(String lockId);

	/**
	 * 查询最小的待发表文章的发布日期
	 * 
	 * @return 如果当前没有任何带发表文章，那么返回null
	 */
	Timestamp selectMinimumScheduleDate();

}
