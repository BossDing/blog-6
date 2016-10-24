package me.qyh.blog.lock;

import java.io.Serializable;

public class LockBean implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Lock lock;
	private String redirectUrl;
	private LockResource lockResource;

	public LockBean(Lock lock, LockResource lockResource, String redirectUrl) {
		this.lock = lock;
		this.redirectUrl = redirectUrl;
		this.lockResource = lockResource;
	}

	public Lock getLock() {
		return lock;
	}

	public String getRedirectUrl() {
		return redirectUrl;
	}

	public LockResource getLockResource() {
		return lockResource;
	}

}
