package me.qyh.blog.web.comment;

import me.qyh.blog.comment.vo.CommentStatistics;

public class _CommentStatistics {

	private final CommentStatistics statistics;

	public _CommentStatistics(CommentStatistics statistics) {
		super();
		this.statistics = statistics;
	}

	public int getTotalArticleComments() {
		return getComments("article");
	}

	public int getTotalPageComments() {
		return getComments("userpage");
	}

	public int getComments(String key) {
		Integer count = statistics.getStMap().get(key);
		return count == null ? 0 : count;
	}

}
