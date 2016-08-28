package me.qyh.blog.ui;

public class TplRenderException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private TplRenderErrorDescription renderErrorDescription;

	public TplRenderException(TplRenderErrorDescription description) {
		this.renderErrorDescription = description;
	}

	public TplRenderErrorDescription getRenderErrorDescription() {
		return renderErrorDescription;
	}
}