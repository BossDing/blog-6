package me.qyh.blog.evt;

import org.springframework.context.ApplicationEvent;

import me.qyh.blog.ui.page.UserPage;

public class UserPageEvent extends ApplicationEvent {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final EventType type;
	private final UserPage deleted;

	public UserPageEvent(Object source, EventType type, UserPage deleted) {
		super(source);
		this.type = type;
		this.deleted = deleted;
	}

	public EventType getType() {
		return type;
	}

	public UserPage getDeleted() {
		return deleted;
	}

}
