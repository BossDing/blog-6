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
package me.qyh.blog.comment;

import java.sql.Timestamp;
import java.util.List;

import org.apache.ibatis.annotations.Param;

import me.qyh.blog.entity.Article;
import me.qyh.blog.entity.Space;

/**
 * 
 * @author Administrator
 *
 */
public interface CommentDao {

	/**
	 * 根据id查询评论
	 * 
	 * @param id
	 *            评论id
	 * @return 如果不存在返回null，否则返回存在的评论
	 */
	Comment selectById(Integer id);

	/**
	 * 插入评论
	 * 
	 * @param comment
	 *            待插入的评论
	 */
	void insert(Comment comment);

	/**
	 * 根据路径和状态删除对应的评论
	 * 
	 * @param path
	 *            路径
	 * @param status
	 *            状态
	 */
	void deleteByPath(String path);

	/**
	 * 根据id删除评论
	 * 
	 * @param id
	 *            评论id
	 * @return 受影响的数目
	 */
	void deleteById(Integer id);

	/**
	 * 查询某个ip在指定时间内评论的总数
	 * 
	 * @param begin
	 *            开始时间
	 * @param end
	 *            结束时间
	 * @param user
	 *            用户
	 * @return 评论数
	 */
	int selectCountByIpAndDatePeriod(@Param("begin") Timestamp begin, @Param("end") Timestamp end,
			@Param("ip") String ip);

	/**
	 * 查询评论数(tree形式)
	 * 
	 * @param param
	 *            查询参数
	 * @return 评论数
	 */
	int selectCountWithTree(CommentQueryParam param);

	/**
	 * 查询评论数(list形式)
	 * 
	 * @param param
	 *            查询参数
	 * @return 评论数
	 */
	int selectCountWithList(CommentQueryParam param);

	/**
	 * 查询评论(tree形式)
	 * 
	 * @param param
	 *            查询参数
	 * @return tree形式的结果集
	 */
	List<Comment> selectPageWithTree(CommentQueryParam param);

	/**
	 * 查询评论(list形式)
	 * 
	 * @param param
	 *            查询参数
	 * @return 结果集
	 */
	List<Comment> selectPageWithList(CommentQueryParam param);

	/**
	 * 查询当前评论的最后一条回复记录
	 * 
	 * @param current
	 *            评论
	 * @return 如果不存在返回null，否在返回最后一条记录
	 */
	Comment selectLast(Comment current);

	/**
	 * 查询某个空间下最后的几条评论
	 * 
	 * @param space
	 *            空间
	 * @param limit
	 *            总数
	 * @param queryPrivate
	 *            是否查询私有空间|文章下的评论
	 * @return 评论集
	 */
	List<Comment> selectLastComments(@Param("space") Space space, @Param("limit") int limit,
			@Param("queryPrivate") boolean queryPrivate);

	/**
	 * 更具文章和ip删除评论
	 * 
	 * @param ip
	 *            用户ip
	 * @param article
	 *            文章
	 * @param status
	 *            评论状态
	 */
	void deleteByIpAndArticle(@Param("ip") String ip, @Param("article") Article article);

	/**
	 * 删除文章下的所有评论
	 * 
	 * @param article
	 *            文章
	 */
	void deleteByArticle(Article article);

	/**
	 * 将评论状态由审核变为普通
	 * 
	 * @param comment
	 *            评论
	 */
	void updateStatusToNormal(Comment comment);

	/**
	 * 查询文章的评论数
	 * 
	 * @param ids
	 * @return
	 */
	List<ArticleComments> selectArticlesCommentCount(List<Integer> ids);

	/**
	 * 查询文章评论数
	 * 
	 * @param id
	 *            文章id
	 * @return 评论数
	 */
	int selectArticleCommentCount(Integer id);

	int selectArticlesTotalCommentCount(@Param("space") Space space, @Param("queryPrivate") boolean queryPrivate,
			@Param("queryHidden") boolean queryHidden);

	public class ArticleComments {
		private Integer id;
		private Integer comments;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public Integer getComments() {
			return comments;
		}

		public void setComments(Integer comments) {
			this.comments = comments;
		}

	}

}
