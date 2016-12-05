/*
 * Copyright 2016 qyh.me
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package me.qyh.blog.comment;

import java.sql.Timestamp;
import java.util.List;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;

import me.qyh.blog.entity.Article;
import me.qyh.blog.entity.BaseEntity;
import me.qyh.blog.message.Message;

/**
 * 
 * @author Administrator
 *
 */
public class Comment extends BaseEntity {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private Comment parent;// 如果为null,则为评论，否则为回复
	@JsonIgnore
	private String parentPath;// 路径，最多支持255个字符(索引原因)
	private String content;
	@JsonIgnore
	private Article article;// 文章
	private List<Integer> parents = Lists.newArrayList();
	private Timestamp commentDate;
	private List<Comment> children = Lists.newArrayList();
	private CommentStatus status;

	private String website;
	private String nickname;
	private String email;
	private String ip;
	private Boolean admin;// 是否是管理员

	private String gravatar;

	/**
	 * 评论状态
	 * 
	 * @author Administrator
	 *
	 */
	public enum CommentStatus {
		NORMAL(new Message("comment.status.normal", "正常")), CHECK(new Message("comment.status.check", "审核"));

		private Message message;

		private CommentStatus(Message message) {
			this.message = message;
		}

		public Message getMessage() {
			return message;
		}
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
		if (!"/".equals(parentPath)) {
			for (String p : Splitter.on('/').split(parentPath)) {
				if (!p.isEmpty()) {
					this.parents.add(Integer.parseInt(p));
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

	public CommentStatus getStatus() {
		return status;
	}

	public void setStatus(CommentStatus status) {
		this.status = status;
	}

	public boolean isChecking() {
		return CommentStatus.CHECK.equals(status);
	}

	public String getNickname() {
		return nickname;
	}

	public void setNickname(String nickname) {
		this.nickname = nickname;
	}

	@JsonIgnore
	public String getEmail() {
		return email;
	}

	@JsonProperty
	public void setEmail(String email) {
		this.email = email;
	}

	@JsonIgnore
	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public String getWebsite() {
		return website;
	}

	public void setWebsite(String website) {
		this.website = website;
	}

	public String getGravatar() {
		return gravatar;
	}

	public void setGravatar(String gravatar) {
		this.gravatar = gravatar;
	}

	public Boolean getAdmin() {
		return admin;
	}

	public void setAdmin(Boolean admin) {
		this.admin = admin;
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
		Comment rhs = (Comment) obj;
		return new EqualsBuilder().append(id, rhs.id).isEquals();
	}
}
