package me.qyh.blog.pageparam;

import java.util.Date;

import org.springframework.format.annotation.DateTimeFormat;

import me.qyh.blog.entity.Article.ArticleFrom;
import me.qyh.blog.entity.Article.ArticleStatus;
import me.qyh.util.Validators;
import me.qyh.blog.entity.Space;

public class ArticleQueryParam extends PageQueryParam {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private Space space;
	@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private Date begin;
	@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private Date end;
	private String query;
	private ArticleStatus status;
	private ArticleFrom from;
	private boolean ignoreLevel;// 忽略置顶
	private boolean queryPrivate;// 查询私人博客
	private String tag;
	private Boolean hasLock;
	private Sort sort;
	private boolean querySpacePrivate;// 如果为是，查询全部，如果为否，不查询空间私有文章(如果space==null)

	public enum Sort {
		HITS, COMMENTS
	}

	public Space getSpace() {
		return space;
	}

	public void setSpace(Space space) {
		this.space = space;
		if(space != null)
			this.querySpacePrivate = true;
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

	public String getQuery() {
		return query;
	}

	public void setQuery(String query) {
		this.query = query;
	}

	public ArticleStatus getStatus() {
		return status;
	}

	public void setStatus(ArticleStatus status) {
		this.status = status;
	}

	public boolean isIgnoreLevel() {
		return ignoreLevel;
	}

	public void setIgnoreLevel(boolean ignoreLevel) {
		this.ignoreLevel = ignoreLevel;
	}

	public ArticleFrom getFrom() {
		return from;
	}

	public void setFrom(ArticleFrom from) {
		this.from = from;
	}

	public String getTag() {
		return tag;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}

	public boolean isQueryPrivate() {
		return queryPrivate;
	}

	public void setQueryPrivate(boolean queryPrivate) {
		this.queryPrivate = queryPrivate;
	}

	public Boolean getHasLock() {
		return hasLock;
	}

	public void setHasLock(Boolean hasLock) {
		this.hasLock = hasLock;
	}

	public Sort getSort() {
		return sort;
	}

	public void setSort(Sort sort) {
		this.sort = sort;
	}

	public boolean hasQuery() {
		return !Validators.isEmptyOrNull(query, true);
	}

	public boolean isQuerySpacePrivate() {
		return querySpacePrivate;
	}

	public void setQuerySpacePrivate(boolean querySpacePrivate) {
		this.querySpacePrivate = querySpacePrivate;
	}

	@Override
	public String toString() {
		return "ArticleQueryParam [space=" + space + ", begin=" + begin + ", end=" + end + ", query=" + query
				+ ", status=" + status + ", from=" + from + ", ignoreLevel=" + ignoreLevel + ", queryPrivate="
				+ queryPrivate + ", tag=" + tag + ", hasLock=" + hasLock + ", sort=" + sort + "]";
	}

}
