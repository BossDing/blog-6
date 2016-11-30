package me.qyh.blog.service.impl;

import me.qyh.blog.exception.LogicException;

public interface CommentContentChecker {
	/**
	 * 检查评论内容
	 * 
	 * @param content
	 *            内容
	 * @param html
	 *            是否是html文本
	 * @throws LogicException
	 *             检查未通过
	 */
	void doCheck(final String content, boolean html) throws LogicException;
}