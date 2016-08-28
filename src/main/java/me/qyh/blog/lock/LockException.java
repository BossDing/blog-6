package me.qyh.blog.lock;

public class LockException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private Lock lock;

	public Lock getLock() {
		return lock;
	}

	public LockException(Lock lock) {
		this.lock = lock;
	}

}
