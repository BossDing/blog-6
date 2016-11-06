package me.qyh.blog.pageparam;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;

import me.qyh.blog.entity.Article.ArticleFrom;
import me.qyh.blog.entity.Article.ArticleStatus;
import me.qyh.blog.entity.Space;
import me.qyh.util.Validators;

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
	private List<ArticleStatus> statuses = new ArrayList<ArticleStatus>();
	private ArticleFrom from;
	private boolean ignoreLevel;// 忽略置顶
	private boolean queryPrivate;// 查询私人博客
	private String tag;
	private Boolean hasLock;
	private Sort sort;
	private boolean queryHidden;// 如果为是，查询全部，如果为否，不查询空间私有文章(如果space==null)

	public enum Sort {
		HITS, COMMENTS
	}

	public Space getSpace() {
		return space;
	}

	public void setSpace(Space space) {
		this.space = space;
		if (space != null)
			this.queryHidden = true;
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

	public List<ArticleStatus> getStatuses() {
		return statuses;
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

	public void setStatuses(List<ArticleStatus> statuses) {
		this.statuses = statuses;
	}

	public void setStatuses(ArticleStatus... statuses) {
		this.statuses = Arrays.asList(statuses);
	}

	public boolean isQueryHidden() {
		return queryHidden;
	}

	public void setQueryHidden(boolean queryHidden) {
		this.queryHidden = queryHidden;
	}

	public String getStatusStr() {
		if (this.status != null)
			return status.name();
		StringBuilder sb = new StringBuilder();
		for (ArticleStatus status : statuses)
			sb.append(status.name()).append(",");
		if (sb.length() > 1)
			sb.deleteCharAt(sb.length() - 1);
		return sb.toString();
	}

	public boolean hasStatus(String statusStr) {
		if (this.status != null) {
			return statusStr.equals(status.name());
		}
		for (ArticleStatus status : statuses)
			if (statusStr.equals(status.name()))
				return true;
		return false;
	}

}
