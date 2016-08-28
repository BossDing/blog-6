package me.qyh.blog.ui.widget;

import java.util.Date;

import org.apache.commons.lang3.time.DateFormatUtils;

/**
 * 文档日期归档
 * 
 * @author Administrator
 *
 */
public class ArticleDateFile {

	private Date begin;
	private Date end;
	private int count;

	public Date getBegin() {
		return begin;
	}

	public void setBegin(Date begin) {
		this.begin = begin;
	}

	public Date getEnd() {
		return end;
	}

	public void setEnd(Date end) {
		this.end = end;
	}

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}

	@Override
	public String toString() {
		return "ArticleFile [begin=" + DateFormatUtils.format(begin, "yyyy-MM-dd") + ", end="
				+ DateFormatUtils.format(end, "yyy-MM-dd") + ", count=" + count + "]";
	}

}
