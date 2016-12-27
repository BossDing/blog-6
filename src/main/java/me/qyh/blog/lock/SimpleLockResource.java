package me.qyh.blog.lock;

public class SimpleLockResource implements LockResource {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final String resourceId;
	private final String lockId;

	public SimpleLockResource(String resourceId, String lockId) {
		super();
		this.resourceId = resourceId;
		this.lockId = lockId;
	}

	@Override
	public String getResourceId() {
		return resourceId;
	}

	@Override
	public String getLockId() {
		return lockId;
	}

}
