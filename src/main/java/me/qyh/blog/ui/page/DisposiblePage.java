package me.qyh.blog.ui.page;

/**
 * 这是一个单独的页面，用来处理单个fragament的渲染以及页面的预览
 * 
 * @author Administrator
 *
 */
public class DisposiblePage extends Page {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * if true load preview data tag
	 */
	private boolean preview = true;

	public DisposiblePage() {
		super();
	}

	public DisposiblePage(Page page) {
		super(page);
	}

	@Override
	public final PageType getType() {
		return PageType.DISPOSIBLE;
	}

	public boolean isPreview() {
		return preview;
	}

	public void setPreview(boolean preview) {
		this.preview = preview;
	}

}
