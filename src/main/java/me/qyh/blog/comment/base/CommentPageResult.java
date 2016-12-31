package me.qyh.blog.comment.base;

import java.util.List;

import me.qyh.blog.pageparam.PageQueryParam;
import me.qyh.blog.pageparam.PageResult;

public final class CommentPageResult<E extends BaseComment<E>> extends PageResult<E> {
	private final CommentConfig commentConfig;

	public CommentPageResult(PageQueryParam param, int totalRow, List<E> datas, CommentConfig commentConfig) {
		super(param, totalRow, datas);
		this.commentConfig = commentConfig;
	}

	public CommentConfig getCommentConfig() {
		return commentConfig;
	}
}