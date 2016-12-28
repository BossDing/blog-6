package me.qyh.blog.evt;

import org.springframework.context.ApplicationEvent;

/**
 * 锁删除事件
 * 
 * @author mhlx
 *
 */
public final class LockDeleteEvent extends ApplicationEvent {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final String lockId;

	public LockDeleteEvent(Object source, String lockId) {
		super(source);
		this.lockId = lockId;
	}

	public String getLockId() {
		return lockId;
	}

}
