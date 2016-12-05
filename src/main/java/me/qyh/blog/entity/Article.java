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
package me.qyh.blog.entity;

import java.sql.Timestamp;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.collect.Sets;

import me.qyh.blog.message.Message;

/**
 * 
 * @author Administrator
 *
 */
public class Article extends BaseLockResource {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private Space space;// 空间
	private String title;// 标题
	private String content;// 博客原始内容
	private Set<Tag> tags = Sets.newLinkedHashSet();// 博客标签
	@JsonDeserialize(using = DateDeserializer.class)
	private Timestamp pubDate;// 撰写日期
	private Timestamp lastModifyDate;// 最后修改日期
	private Boolean isPrivate;// 是否是私人博客
	private int hits;// 点击数量
	private int comments;// 评论数量
	private ArticleFrom from;// 博客来源
	private ArticleStatus status;// 博客状态
	private Editor editor;// 编辑器
	private String summary;// 博客摘要
	private Integer level; // 博客级别，级别越高显示越靠前
	private String alias;// 别名，可通过别名访问文章
	private Boolean allowComment;

	private AtomicInteger atomicHits;

	/**
	 * <b>设置该属性的文章不会被非该空间查询、统计到</b>
	 * </p>
	 */
	private Boolean hidden;

	/**
	 * 文章来源
	 * 
	 * @author Administrator
	 *
	 */
	public enum ArticleFrom {
		// 原创
		ORIGINAL(new Message("article.from.original", "原创")),
		// 转载
		COPIED(new Message("article.from.copied", "转载"));

		private Message message;

		private ArticleFrom(Message message) {
			this.message = message;
		}

		private ArticleFrom() {

		}

		public Message getMessage() {
			return message;
		}
	}

	/**
	 * 文章状态
	 * 
	 * @author Administrator
	 *
	 */
	public enum ArticleStatus {
		// 正常
		PUBLISHED(new Message("article.status.published", "已发布")),
		// 计划博客
		SCHEDULED(new Message("article.status.schedule", "计划中")),
		// 草稿
		DRAFT(new Message("article.status.draft", "草稿")),
		// 回收站
		DELETED(new Message("article.status.deleted", "已删除"));

		private Message message;

		private ArticleStatus(Message message) {
			this.message = message;
		}

		private ArticleStatus() {

		}

		public Message getMessage() {
			return message;
		}
	}

	/**
	 * default
	 */
	public Article() {
		super();
	}

	/**
	 * 
	 * @param id
	 *            文章id
	 */
	public Article(Integer id) {
		super(id);
	}

	/**
	 * clone
	 * 
	 * @param source
	 *            源文章
	 */
	public Article(Article source) {
		this.atomicHits = source.atomicHits;
		this.alias = source.alias;
		this.comments = source.comments;
		this.content = source.content;
		this.editor = source.editor;
		this.from = source.from;
		this.hidden = source.hidden;
		this.hits = source.hits;
		this.isPrivate = source.isPrivate;
		this.lastModifyDate = source.lastModifyDate;
		this.level = source.level;
		this.pubDate = source.pubDate;
		this.space = source.space;
		this.status = source.status;
		this.summary = source.summary;
		this.tags = source.tags;
		this.title = source.title;
		this.id = source.id;
	}

	public Space getSpace() {
		return space;
	}

	public void setSpace(Space space) {
		this.space = space;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public Set<Tag> getTags() {
		return tags;
	}

	public void setTags(Set<Tag> tags) {
		this.tags = tags;
	}

	public Timestamp getPubDate() {
		return pubDate;
	}

	public void setPubDate(Timestamp pubDate) {
		this.pubDate = pubDate;
	}

	public Timestamp getLastModifyDate() {
		return lastModifyDate;
	}

	public void setLastModifyDate(Timestamp lastModifyDate) {
		this.lastModifyDate = lastModifyDate;
	}

	public Boolean isPrivate() {
		if (isPrivate == null) {
			return false;
		}
		if (!isPrivate && (space != null && space.getIsPrivate() != null)) {
			return space.getIsPrivate();
		}
		return isPrivate;
	}

	public Boolean getIsPrivate() {
		return isPrivate;
	}

	public void setIsPrivate(Boolean isPrivate) {
		this.isPrivate = isPrivate;
	}

	public int getHits() {
		return atomicHits != null ? atomicHits.get() : hits;
	}

	public void setHits(int hits) {
		this.hits = hits;
		this.atomicHits = new AtomicInteger(hits);
	}

	/**
	 * 点击量+1
	 * 
	 * @return 当前点击量
	 */
	public int addHits() {
		return atomicHits.incrementAndGet();
	}

	public int getComments() {
		return comments;
	}

	public void setComments(int comments) {
		this.comments = comments;
	}

	public ArticleFrom getFrom() {
		return from;
	}

	public void setFrom(ArticleFrom from) {
		this.from = from;
	}

	public ArticleStatus getStatus() {
		return status;
	}

	public void setStatus(ArticleStatus status) {
		this.status = status;
	}

	public Editor getEditor() {
		return editor;
	}

	public void setEditor(Editor editor) {
		this.editor = editor;
	}

	public String getSummary() {
		return summary;
	}

	public void setSummary(String summary) {
		this.summary = summary;
	}

	public Integer getLevel() {
		return level;
	}

	public void setLevel(Integer level) {
		this.level = level;
	}

	public boolean isSchedule() {
		return ArticleStatus.SCHEDULED.equals(status);
	}

	public boolean isDeleted() {
		return ArticleStatus.DELETED.equals(status);
	}

	public boolean isPublished() {
		return ArticleStatus.PUBLISHED.equals(status);
	}

	public boolean isDraft() {
		return ArticleStatus.DRAFT.equals(status);
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public String getTagStr() {
		if (CollectionUtils.isEmpty(tags)) {
			return "";
		}
		StringBuilder sb = new StringBuilder();
		for (Tag tag : tags) {
			sb.append(tag.getName()).append(",");
		}
		sb.deleteCharAt(sb.length() - 1);
		return sb.toString();
	}

	@Override
	public boolean hasLock() {
		boolean hasLock = super.hasLock();
		if (!hasLock) {
			hasLock = space != null && space.hasLock();
		}
		return hasLock;
	}

	/**
	 * 是否包含某个标签
	 * 
	 * @param tag
	 *            标签
	 * @return true包含，false不包含
	 */
	public boolean hasTag(String tag) {
		for (Tag _tag : this.tags) {
			if (tag.equals(_tag.getName())) {
				return true;
			}
		}
		return false;
	}

	public String getAlias() {
		return alias;
	}

	public void setAlias(String alias) {
		this.alias = alias;
	}

	public Boolean getHidden() {
		return hidden;
	}

	public void setHidden(Boolean hidden) {
		this.hidden = hidden;
	}

	public Boolean getAllowComment() {
		return allowComment;
	}

	public void setAllowComment(Boolean allowComment) {
		this.allowComment = allowComment;
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder().append(id).build();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (obj == this) {
			return true;
		}
		if (obj.getClass() != getClass()) {
			return false;
		}
		Article rhs = (Article) obj;
		return new EqualsBuilder().append(id, rhs.id).isEquals();
	}
}
