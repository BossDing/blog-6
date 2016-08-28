package me.qyh.blog.lock;

/**
 * 锁丢失
 * 
 * @author Administrator
 *
 */
public class MissLockException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public MissLockException(String message) {
		super(message);
	}
	
	public MissLockException() {
		super();
	}

}
