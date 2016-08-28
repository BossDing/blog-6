package me.qyh.blog.exception;

public class SpaceNotFoundException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String alias;

	public SpaceNotFoundException(String alias) {
		this.alias = alias;
	}

	public String getAlias() {
		return alias;
	}

}
