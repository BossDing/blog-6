package me.qyh.blog.pageparam;

import java.io.Serializable;

public abstract class PageQueryParam implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private int currentPage;
	private int pageSize;

	public int getOffset() {
		return PageResult.countOffset(currentPage, pageSize);
	}

	public int getPageSize() {
		return pageSize;
	}

	public void setPageSize(int pageSize) {
		this.pageSize = pageSize;
	}

	public void setCurrentPage(int currentPage) {
		this.currentPage = currentPage;
	}

	public int getCurrentPage() {
		return currentPage;
	}

	public PageQueryParam() {
	}

	public PageQueryParam(int currentPage, int pageSize) {
		this.currentPage = currentPage;
		this.pageSize = pageSize;
	}
}
