package me.qyh.blog.entity;

import java.sql.Timestamp;

public class Space extends BaseLockResource {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * 
	 */
	private String name;// 空间名
	private String alias;
	private Timestamp createDate;// 创建时间
	private Boolean isPrivate;

	private Boolean articleHidden;
	private CommentConfig commentConfig;

	private Boolean isDefault;

	public String getAlias() {
		return alias;
	}

	public void setAlias(String alias) {
		this.alias = alias;
	}

	public Timestamp getCreateDate() {
		return createDate;
	}

	public void setCreateDate(Timestamp createDate) {
		this.createDate = createDate;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Space() {
		super();
	}

	public Space(Integer id) {
		super(id);
	}

	@Override
	public String getResourceId() {
		return "Space-" + alias;
	}

	public Boolean getIsPrivate() {
		return isPrivate;
	}

	public void setIsPrivate(Boolean isPrivate) {
		this.isPrivate = isPrivate;
	}

	public Boolean getArticleHidden() {
		return articleHidden;
	}

	public void setArticleHidden(Boolean articleHidden) {
		this.articleHidden = articleHidden;
	}

	public CommentConfig getCommentConfig() {
		return commentConfig;
	}

	public void setCommentConfig(CommentConfig commentConfig) {
		this.commentConfig = commentConfig;
	}

	public Boolean getIsDefault() {
		return isDefault;
	}

	public void setIsDefault(Boolean isDefault) {
		this.isDefault = isDefault;
	}

	@Override
	public String toString() {
		return "Space [name=" + name + ", alias=" + alias + ", createDate=" + createDate + ", isPrivate=" + isPrivate
				+ ", articleHidden=" + articleHidden + ", commentConfig=" + commentConfig + "]";
	}
}
