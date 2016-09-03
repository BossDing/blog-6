package me.qyh.blog.entity;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import me.qyh.blog.input.JsonHtmlXssSerializer;
import me.qyh.blog.oauth2.OauthUser;

public class Comment extends Id {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private Comment parent;// 如果为null,则为评论，否则为回复
	@JsonIgnore
	private String parentPath;// 路径，最多支持255个字符(索引原因)
	@JsonSerialize(using = JsonHtmlXssSerializer.class)
	private String content;
	private OauthUser user;
	@JsonIgnore
	private Article article;// 文章
	private List<Integer> parents = new ArrayList<Integer>();
	private Timestamp commentDate;
	private List<Comment> children = new ArrayList<Comment>();

	@JsonIgnore
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
		if (!parentPath.equals("/")) {
			String[] _parents = parentPath.split("/");
			for (String _parent : _parents) {
				if (!_parent.isEmpty()) {
					parents.add(Integer.parseInt(_parent));
				}
			}
		}
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

	public boolean isRoot() {
		return parent == null;
	}

	public List<Integer> getParents() {
		return parents;
	}

	public Timestamp getCommentDate() {
		return commentDate;
	}

	public void setCommentDate(Timestamp commentDate) {
		this.commentDate = commentDate;
	}

	public List<Comment> getChildren() {
		return children;
	}

	public void setChildren(List<Comment> children) {
		this.children = children;
	}

}
