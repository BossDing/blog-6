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

public interface CommentDao {

	Comment selectById(Integer id);

	void insert(Comment comment);

	int deleteByPath(@Param("path") String path, @Param("status") CommentStatus status);

	int deleteById(Integer id);

	int selectCountByUserAndDatePeriod(@Param("begin") Timestamp begin, @Param("end") Timestamp end,
			@Param("user") OauthUser user);

	int selectCountWithTree(CommentQueryParam param);

	int selectCountWithList(CommentQueryParam param);

	List<Comment> selectPageWithTree(CommentQueryParam param);

	List<Comment> selectPageWithList(CommentQueryParam param);

	Comment selectLast(Comment current);

	List<Comment> selectLastComments(@Param("space") Space space, @Param("limit") int limit,
			@Param("queryPrivate") boolean queryPrivate);

	int deleteByUserAndArticle(@Param("user") OauthUser user, @Param("article") Article article,
			@Param("status") CommentStatus status);

	int deleteByArticle(Article article);

	int updateStatusToNormal(Comment comment);
}
