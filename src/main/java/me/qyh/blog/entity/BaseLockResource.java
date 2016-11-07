package me.qyh.blog.entity;

import me.qyh.blog.lock.LockResource;

public abstract class BaseLockResource extends Id implements LockResource {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private String lockId;

	@Override
	public String getResourceId() {
		return this.getClass().getSimpleName() + ":" + getId();
	}

	@Override
	public String getLockId() {
		return lockId;
	}

	public void setLockId(String lockId) {
		this.lockId = lockId;
	}

	public boolean hasLock() {
		return (lockId != null);
	}

	public BaseLockResource() {
		super();
	}

	public BaseLockResource(Integer id) {
		super(id);
	}

}
