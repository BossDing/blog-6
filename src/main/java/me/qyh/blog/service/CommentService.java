package me.qyh.blog.service;

import java.util.List;

import me.qyh.blog.entity.Comment;
import me.qyh.blog.entity.Space;
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.pageparam.CommentQueryParam;
import me.qyh.blog.pageparam.PageResult;

public interface CommentService {

	Comment insertComment(Comment comment) throws LogicException;

	void deleteComment(Integer id) throws LogicException;

	void deleteComment(Integer userId, Integer articleId) throws LogicException;

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

}
