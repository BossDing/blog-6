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

import me.qyh.blog.entity.Article;
import me.qyh.blog.entity.Comment;
import me.qyh.blog.entity.Comment.CommentStatus;
import me.qyh.blog.entity.Space;
import me.qyh.blog.oauth2.OauthUser;
import me.qyh.blog.pageparam.CommentQueryParam;

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
	 * @return 受影响的数目
	 */
	int deleteByPath(@Param("path") String path, @Param("status") CommentStatus status);

	/**
	 * 根据id删除评论
	 * 
	 * @param id
	 *            评论id
	 * @return 受影响的数目
	 */
	int deleteById(Integer id);

	/**
	 * 查询某个用户在指定时间内评论的总数
	 * 
	 * @param begin
	 *            开始时间
	 * @param end
	 *            结束时间
	 * @param user
	 *            用户
	 * @return 评论数
	 */
	int selectCountByUserAndDatePeriod(@Param("begin") Timestamp begin, @Param("end") Timestamp end,
			@Param("user") OauthUser user);

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
	 * 更具文章和用户删除评论
	 * 
	 * @param user
	 *            用户
	 * @param article
	 *            文章
	 * @param status
	 *            评论状态
	 * @return 受影响的纪录数
	 */
	int deleteByUserAndArticle(@Param("user") OauthUser user, @Param("article") Article article,
			@Param("status") CommentStatus status);

	/**
	 * 删除文章下的所有评论
	 * 
	 * @param article
	 *            文章
	 * @return 受影响的记录数
	 */
	int deleteByArticle(Article article);

	/**
	 * 将评论状态由审核变为普通
	 * 
	 * @param comment
	 *            评论
	 * @return 受影响的记录数
	 */
	int updateStatusToNormal(Comment comment);
}
