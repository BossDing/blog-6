package me.qyh.blog.bean;

import me.qyh.blog.entity.Space;

/**
 * 文章按照空间归档
 * 
 * @author Administrator
 *
 */
public class ArticleSpaceFile {

	private Space space;
	private int count;

	public Space getSpace() {
		return space;
	}

	public void setSpace(Space space) {
		this.space = space;
	}

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}

}
