package me.qyh.blog.core.evt;

import org.springframework.context.ApplicationEvent;

import me.qyh.blog.core.entity.Space;

/**
 * 空间删除事件
 * 
 * @author mhlx
 *
 */
public final class SpaceDeleteEvent extends ApplicationEvent {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final Space space;// 被删除的空间

	public SpaceDeleteEvent(Object source, Space space) {
		super(source);
		this.space = space;
	}

	public Space getSpace() {
		return space;
	}

}
