package me.qyh.blog.lock;

import me.qyh.blog.message.Message;

public class LockException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private Lock lock;
	private LockResource lockResource;
	private Message error;

	public LockException(Lock lock, LockResource lockResource, Message error) {
		super();
		this.lock = lock;
		this.lockResource = lockResource;
		this.error = error;
	}

	public Lock getLock() {
		return lock;
	}

	public LockResource getLockResource() {
		return lockResource;
	}

	public Message getError() {
		return error;
	}

}
