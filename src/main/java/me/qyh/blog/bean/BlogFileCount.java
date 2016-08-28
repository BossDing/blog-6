package me.qyh.blog.bean;

import me.qyh.blog.entity.BlogFile.BlogFileType;

public class BlogFileCount {

	private BlogFileType type;
	private int count;

	public BlogFileType getType() {
		return type;
	}

	public void setType(BlogFileType type) {
		this.type = type;
	}

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}

}
