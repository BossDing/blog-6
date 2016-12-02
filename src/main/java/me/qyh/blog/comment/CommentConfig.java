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
package me.qyh.blog.comment;

import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import me.qyh.blog.config.Limit;
import me.qyh.blog.message.Message;

/**
 * 
 * @author Administrator
 *
 */
public class CommentConfig {

	private Boolean allowHtml;
	private Boolean asc;
	private CommentMode commentMode;
	private Integer limitCount;
	private Integer limitSec;
	private Boolean check;// 审核
	private Integer pageSize;// 每页显示数量

	public CommentConfig() {
		super();
	}

	public CommentConfig(CommentConfig source) {
		this.allowHtml = source.allowHtml;
		this.asc = source.asc;
		this.check = source.check;
		this.commentMode = source.commentMode;
		this.limitCount = source.limitCount;
		this.limitSec = source.limitSec;
		this.pageSize = source.pageSize;
	}

	/**
	 * 展现方式
	 * 
	 * @author Administrator
	 *
	 */
	public enum CommentMode {
		LIST(new Message("article.commentMode.list", "平铺")), TREE(new Message("article.commentMode.tree", "嵌套"));

		private Message message;

		private CommentMode(Message message) {
			this.message = message;
		}

		private CommentMode() {

		}

		public Message getMessage() {
			return message;
		}
	}

	public Boolean getAllowHtml() {
		return allowHtml;
	}

	public void setAllowHtml(Boolean allowHtml) {
		this.allowHtml = allowHtml;
	}

	public Boolean getAsc() {
		return asc;
	}

	public void setAsc(Boolean asc) {
		this.asc = asc;
	}

	public CommentMode getCommentMode() {
		return commentMode;
	}

	public void setCommentMode(CommentMode commentMode) {
		this.commentMode = commentMode;
	}

	@JsonIgnore
	public Integer getLimitSec() {
		return limitSec;
	}
	
	@JsonProperty
	public void setLimitSec(Integer limitSec) {
		this.limitSec = limitSec;
	}

	@JsonIgnore
	public Integer getLimitCount() {
		return limitCount;
	}

	@JsonProperty
	public void setLimitCount(Integer limitCount) {
		this.limitCount = limitCount;
	}

	@JsonIgnore
	public Limit getLimit() {
		return new Limit(limitCount, limitSec, TimeUnit.SECONDS);
	}

	public Boolean getCheck() {
		return check;
	}

	public void setCheck(Boolean check) {
		this.check = check;
	}

	public Integer getPageSize() {
		return pageSize;
	}

	public void setPageSize(Integer pageSize) {
		this.pageSize = pageSize;
	}
}