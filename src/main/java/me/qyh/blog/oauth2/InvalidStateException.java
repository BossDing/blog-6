package me.qyh.blog.oauth2;

/**
 * 非法状态码
 * 
 * @author 钱宇豪
 * @date 2016年8月29日 上午10:58:00
 */
public class InvalidStateException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public InvalidStateException(String msg) {
		super(msg);
	}

}
