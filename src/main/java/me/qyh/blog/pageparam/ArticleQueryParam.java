/*
 * Copyright 2016 qyh.me
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package me.qyh.blog.pageparam;

import java.util.Date;

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
	private ArticleFrom from;
	private boolean ignoreLevel;// 忽略置顶
	private boolean queryPrivate;// 查询私人博客
	private String tag;
	private Boolean hasLock;
	private Sort sort;
	private boolean queryHidden;
	private boolean highlight = true;// 查询是否高亮显示

	public enum Sort {
		HITS, PUBDATE
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

	public boolean isQueryHidden() {
		return queryHidden;
	}

	public void setQueryHidden(boolean queryHidden) {
		this.queryHidden = queryHidden;
	}

	public boolean isHighlight() {
		return highlight;
	}

	public void setHighlight(boolean highlight) {
		this.highlight = highlight;
	}

}
