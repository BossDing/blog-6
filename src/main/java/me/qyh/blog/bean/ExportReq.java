package me.qyh.blog.bean;

import me.qyh.blog.entity.Space;

public class ExportReq {

	private boolean exportExpandedPage;
	private Space space;

	public boolean isExportExpandedPage() {
		return exportExpandedPage;
	}

	public void setExportExpandedPage(boolean exportExpandedPage) {
		this.exportExpandedPage = exportExpandedPage;
	}

	public Space getSpace() {
		return space;
	}

	public void setSpace(Space space) {
		this.space = space;
	}

}
