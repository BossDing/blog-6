package me.qyh.blog.core.evt;

import org.springframework.context.ApplicationEvent;

import me.qyh.blog.core.entity.Comment;

/**
 * 当有新的评论时才会被触发，评论状态的变更、删除不会触发这个事件
 * 
 * @author mhlx
 *
 */
public class CommentEvent extends ApplicationEvent {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final Comment comment;

	public CommentEvent(Object source, Comment comment) {
		super(source);
		this.comment = comment;
	}

	public Comment getComment() {
		return comment;
	}

}
