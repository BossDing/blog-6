package me.qyh.blog.service;

import me.qyh.blog.entity.Comment;
import me.qyh.blog.exception.LogicException;

public interface CommentService {

	void insertComment(Comment comment) throws LogicException;

	void deleteComment(Integer id) throws LogicException;

}
