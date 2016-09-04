package me.qyh.blog.service.impl;

import me.qyh.blog.exception.LogicException;

public interface CommentContentChecker {

	void doCheck(String content, boolean html) throws LogicException;

}
