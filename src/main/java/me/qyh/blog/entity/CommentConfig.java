package me.qyh.blog.entity;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

import me.qyh.blog.config.Limit;
import me.qyh.blog.message.Message;

public class CommentConfig implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Boolean allowHtml;
	private Boolean allowComment;
	private Boolean asc;
	private CommentMode commentMode;
	private Integer limitCount;
	private Integer limitSec;
	private Boolean check;// 审核

	public enum CommentMode {
		LIST(new Message("article.commentMode.list", "平铺")), TREE(new Message("article.commentMode.tree", "嵌套"));

		private Message message;

		private CommentMode(Message message) {
			this.message = message;
		}

		private CommentMode() {

		}

		public Message getMessage() {
			return message;
		}
	}

	public Boolean getAllowHtml() {
		return allowHtml;
	}

	public void setAllowHtml(Boolean allowHtml) {
		this.allowHtml = allowHtml;
	}

	public Boolean getAllowComment() {
		return allowComment;
	}

	public void setAllowComment(Boolean allowComment) {
		this.allowComment = allowComment;
	}

	public Boolean getAsc() {
		return asc;
	}

	public void setAsc(Boolean asc) {
		this.asc = asc;
	}

	public CommentMode getCommentMode() {
		return commentMode;
	}

	public void setCommentMode(CommentMode commentMode) {
		this.commentMode = commentMode;
	}

	public Integer getLimitSec() {
		return limitSec;
	}

	public void setLimitSec(Integer limitSec) {
		this.limitSec = limitSec;
	}

	public Integer getLimitCount() {
		return limitCount;
	}

	public void setLimitCount(Integer limitCount) {
		this.limitCount = limitCount;
	}

	public Limit getLimit() {
		return new Limit(limitCount, limitSec, TimeUnit.SECONDS);
	}

	public Boolean getCheck() {
		return check;
	}

	public void setCheck(Boolean check) {
		this.check = check;
	}
}