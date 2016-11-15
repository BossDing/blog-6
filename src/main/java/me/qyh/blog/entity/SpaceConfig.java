package me.qyh.blog.entity;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * 
 * @author Administrator
 *
 */
public class SpaceConfig extends BaseEntity {

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

	@Override
	public int hashCode() {
		return new HashCodeBuilder().append(id).build();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (obj == this) {
			return true;
		}
		if (obj.getClass() != getClass()) {
			return false;
		}
		SpaceConfig rhs = (SpaceConfig) obj;
		return new EqualsBuilder().append(id, rhs.id).isEquals();
	}
}
