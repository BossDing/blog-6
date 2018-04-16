package me.qyh.blog.plugin.csrf;

public class EmptyCsrfToken implements CsrfToken {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public String getHeaderName() {
		return "";
	}

	@Override
	public String getParameterName() {
		return "";
	}

	@Override
	public String getToken() {
		return "";
	}

}
