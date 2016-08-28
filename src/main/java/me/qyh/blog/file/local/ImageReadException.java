package me.qyh.blog.file.local;

/**
 * 错误的图片文件
 * 
 * @author Administrator
 *
 */
public class ImageReadException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public ImageReadException(String message, Throwable cause) {
		super(message, cause);
	}

	public ImageReadException(String message) {
		super(message);
	}

}
