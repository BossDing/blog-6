package me.qyh.blog.entity;

public class SpaceConfig extends Id{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private CommentConfig commentConfig;
	private Integer articlePageSize;// 文章每页显示数量

	public CommentConfig getCommentConfig() {
		return commentConfig;
	}

	public void setCommentConfig(CommentConfig commentConfig) {
		this.commentConfig = commentConfig;
	}

	public int getArticlePageSize() {
		return articlePageSize;
	}

	public void setArticlePageSize(int articlePageSize) {
		this.articlePageSize = articlePageSize;
	}

}
