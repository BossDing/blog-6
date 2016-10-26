package me.qyh.blog.ui;

public class TplRenderException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private TplRenderErrorDescription renderErrorDescription;
	private Throwable original;

	public TplRenderException(TplRenderErrorDescription description, Throwable original) {
		this.renderErrorDescription = description;
		this.original = original;
	}

	public TplRenderErrorDescription getRenderErrorDescription() {
		return renderErrorDescription;
	}

	public Throwable getOriginal() {
		return original;
	}

}