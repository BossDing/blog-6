package me.qyh.blog.bean;

import java.util.ArrayList;
import java.util.List;

import me.qyh.blog.entity.BlogFile;
import me.qyh.blog.pageparam.PageResult;

public class BlogFilePageResult {

	private List<BlogFile> paths = new ArrayList<BlogFile>();
	private PageResult<BlogFile> page;

	public List<BlogFile> getPaths() {
		return paths;
	}

	public void setPaths(List<BlogFile> paths) {
		this.paths = paths;
	}

	public PageResult<BlogFile> getPage() {
		return page;
	}

	public void setPage(PageResult<BlogFile> page) {
		this.page = page;
	}

}
