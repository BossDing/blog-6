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

import me.qyh.blog.entity.Comment;
import me.qyh.blog.entity.Space;
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.pageparam.CommentQueryParam;
import me.qyh.blog.pageparam.PageResult;

public interface CommentService {

	/**
	 * 插入评论
	 * 
	 * @param comment
	 * @return
	 * @throws LogicException
	 */
	Comment insertComment(Comment comment) throws LogicException;

	/**
	 * 删除某条评论和该评论的所有回复
	 * 
	 * @param id
	 * @throws LogicException
	 */
	void deleteComment(Integer id) throws LogicException;

	/**
	 * 分页查询评论
	 * 
	 * @param param
	 * @return
	 */
	PageResult<Comment> queryComment(CommentQueryParam param);

	/**
	 * 查询最后几条回复我的评论
	 * 
	 * <p>
	 * <strong>只会查询回复我的评论和回复文章的评论，如果社交账号被标记为admin，那么它作出的任何回复都不会被查询出来</strong>
	 * </p>
	 * 
	 * @return
	 */
	List<Comment> queryLastComments(Space space, int limit);

	/**
	 * 删除某个用户在某篇文章下的所有评论以及评论的回复
	 * 
	 * @param userId
	 * @param articleId
	 * @throws LogicException
	 */
	void deleteComment(Integer userId, Integer articleId) throws LogicException;

	/**
	 * 审核评论
	 * <p>
	 * 审核评论会将该评论的父评论也置为审核通过状态
	 * </p>
	 * 
	 * @param id
	 * @throws LogicException
	 */
	void checkComment(Integer id) throws LogicException;

	/**
	 * 查询对话
	 * 
	 * @param id
	 * @return
	 * @throws LogicException
	 */
	List<Comment> queryConversations(Integer articleId, Integer id) throws LogicException;

}
