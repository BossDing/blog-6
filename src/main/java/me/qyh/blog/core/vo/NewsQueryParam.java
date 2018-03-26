package me.qyh.blog.core.vo;

import java.util.Date;

import org.springframework.format.annotation.DateTimeFormat;

public class NewsQueryParam extends PageQueryParam {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private Date begin;
	@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private Date end;
	private boolean queryPrivate;
	private boolean asc;

	public NewsQueryParam() {
		super();
	}

	public NewsQueryParam(NewsQueryParam param) {
		super(param);
		this.begin = param.begin;
		this.end = param.end;
		this.queryPrivate = param.queryPrivate;
		this.asc = param.asc;
	}

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

	public boolean isQueryPrivate() {
		return queryPrivate;
	}

	public void setQueryPrivate(boolean queryPrivate) {
		this.queryPrivate = queryPrivate;
	}

	public boolean isAsc() {
		return asc;
	}

	public void setAsc(boolean asc) {
		this.asc = asc;
	}

}
