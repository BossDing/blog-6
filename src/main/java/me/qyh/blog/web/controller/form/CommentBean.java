package me.qyh.blog.web.controller.form;

import me.qyh.blog.entity.Comment;

public class CommentBean {

	private Comment comment;
	private String email;

	public Comment getComment() {
		return comment;
	}

	public void setComment(Comment comment) {
		this.comment = comment;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

}
