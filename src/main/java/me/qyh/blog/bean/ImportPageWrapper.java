package me.qyh.blog.bean;

import me.qyh.blog.ui.ExportPage;

public class ImportPageWrapper {

	private int index;// 页面序号
	private ExportPage page;// 页面

	public int getIndex() {
		return index;
	}

	public ImportPageWrapper(int index, ExportPage page) {
		super();
		this.index = index;
		this.page = page;
	}

	public ExportPage getPage() {
		return page;
	}

}
