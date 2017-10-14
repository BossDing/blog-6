package me.qyh.blog.file.store;

/**
 * 用来处理zip文件读取异常
 * <p>
 * 字符串不符等
 * </p>
 * DEBUG level
 * 
 * @author mhlx
 *
 */
public class ZipException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public ZipException(String message, Throwable cause) {
		super(message, cause);
	}

	public ZipException(String message) {
		super(message);
	}

}
