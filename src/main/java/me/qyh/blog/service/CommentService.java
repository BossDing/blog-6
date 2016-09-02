package me.qyh.blog.service;

import me.qyh.blog.entity.Comment;
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.pageparam.CommentQueryParam;
import me.qyh.blog.pageparam.PageResult;

public interface CommentService {

	Comment insertComment(Comment comment) throws LogicException;

	void deleteComment(Integer id) throws LogicException;

	/**
	 * 分页查询评论
	 * 
	 * @param param
	 * @return
	 */
	PageResult<Comment> queryComment(CommentQueryParam param);

}
