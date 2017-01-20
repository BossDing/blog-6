package me.qyh.blog.comment;

import java.util.List;

import me.qyh.blog.pageparam.PageQueryParam;
import me.qyh.blog.pageparam.PageResult;

public final class CommentPageResult extends PageResult<Comment> {
	private final CommentConfig commentConfig;

	public CommentPageResult(PageQueryParam param, int totalRow, List<Comment> datas, CommentConfig commentConfig) {
		super(param, totalRow, datas);
		this.commentConfig = commentConfig;
	}

	public CommentConfig getCommentConfig() {
		return commentConfig;
	}
}