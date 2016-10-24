package me.qyh.blog.lock;

public class LockException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private Lock lock;
	private LockResource lockResource;

	public LockException(Lock lock, LockResource lockResource) {
		super();
		this.lock = lock;
		this.lockResource = lockResource;
	}

	public Lock getLock() {
		return lock;
	}

	public LockResource getLockResource() {
		return lockResource;
	}

}
