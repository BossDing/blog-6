package me.qyh.blog.ui.dialect;

/**
 * 用以重定向页面的异常
 * <p>
 * <b>这个异常不应该被纪录，它仅仅代表着页面需要被跳转，同时也不应该在不需要跳转的时候抛出这个异常</b>
 * </p>
 * 
 * @author Administrator
 *
 */
public class RedirectException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private final String url;
	private final boolean permanently;

	public RedirectException(String url, boolean permanently) {
		super();
		this.url = url;
		this.permanently = permanently;
	}

	public String getUrl() {
		return url;
	}

	public boolean isPermanently() {
		return permanently;
	}

	@Override
	public synchronized Throwable fillInStackTrace() {
		return this;
	}

}
