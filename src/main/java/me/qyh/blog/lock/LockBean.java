package me.qyh.blog.lock;

import java.io.Serializable;

public class LockBean implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Lock lock;
	private String redirectUrl;

	public LockBean(Lock lock, String redirectUrl) {
		this.lock = lock;
		this.redirectUrl = redirectUrl;
	}

	public Lock getLock() {
		return lock;
	}

	public String getRedirectUrl() {
		return redirectUrl;
	}

}
