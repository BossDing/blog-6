package me.qyh.blog.comment.vo;

import me.qyh.blog.comment.entity.CommentModule;

public class ModuleCommentCount {
	private CommentModule module;
	private Integer comments;

	public CommentModule getModule() {
		return module;
	}

	public void setModule(CommentModule module) {
		this.module = module;
	}

	public Integer getComments() {
		return comments;
	}

	public void setComments(Integer comments) {
		this.comments = comments;
	}

	public Integer getModuleId() {
		return module.getId();
	}

}