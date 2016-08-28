package me.qyh.blog.pageparam;

import me.qyh.blog.entity.BlogFile.BlogFileType;

public class BlogFileQueryParam extends PageQueryParam {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Integer parent;
	private BlogFileType type;

	public Integer getParent() {
		return parent;
	}

	public void setParent(Integer parent) {
		this.parent = parent;
	}

	public BlogFileType getType() {
		return type;
	}

	public void setType(BlogFileType type) {
		this.type = type;
	}

}
