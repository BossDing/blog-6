package me.qyh.blog.entity;

public class Comment extends Id {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private Comment parent;// 如果为null,则为评论，否则为回复
	private String parentPath;// 路径，最多支持255个字符(索引原因)
	private String content;
	private OauthUser user;
	private Article article;// 文章

	public String getSubPath() {
		return "/" + getId() + "/";
	}

	public Comment getParent() {
		return parent;
	}

	public void setParent(Comment parent) {
		this.parent = parent;
	}

	public String getParentPath() {
		return parentPath;
	}

	public void setParentPath(String parentPath) {
		this.parentPath = parentPath;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public OauthUser getUser() {
		return user;
	}

	public void setUser(OauthUser user) {
		this.user = user;
	}

	public Article getArticle() {
		return article;
	}

	public void setArticle(Article article) {
		this.article = article;
	}

}
