package me.qyh.blog.evt;

import org.springframework.context.ApplicationEvent;

import me.qyh.blog.entity.Comment;

/**
 * 用户评论后触发
 * 
 * @author Administrator
 *
 */
public class CommentEvent extends ApplicationEvent {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public CommentEvent(Object source, Comment comment) {
		super(source);
		this.comment = comment;
	}

	private Comment comment;

	public Comment getComment() {
		return comment;
	}
}
