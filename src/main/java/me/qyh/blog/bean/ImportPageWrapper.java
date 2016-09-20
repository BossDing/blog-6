package me.qyh.blog.bean;

import me.qyh.blog.ui.page.Page;

public class ImportPageWrapper {

	private int index;// 页面序号
	private Page page;// 页面

	public int getIndex() {
		return index;
	}

	public Page getPage() {
		return page;
	}

	public ImportPageWrapper(int index, Page page) {
		this.index = index;
		this.page = page;
	}

}
