package me.qyh.blog.lock.support;

public class PictureQALock extends QALock {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private String lockType;
	private String picture;

	@Override
	public String getLockType() {
		return lockType;
	}

	public void setLockType(String lockType) {
		this.lockType = lockType;
	}

	public String getPicture() {
		return picture;
	}

	public void setPicture(String picture) {
		this.picture = picture;
	}

}
