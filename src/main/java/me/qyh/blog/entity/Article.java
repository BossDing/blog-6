package me.qyh.blog.entity;

import java.sql.Timestamp;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import me.qyh.blog.exception.SystemException;
import me.qyh.blog.message.Message;

public class Article extends BaseLockResource implements Cloneable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private Space space;// 空间
	private String title;// 标题
	private String content;// 博客原始内容
	private Set<Tag> tags = new LinkedHashSet<Tag>();// 博客标签
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
	private CommentConfig commentConfig;
	private String alias;// 别名，可通过别名访问文章

	private AtomicInteger _hits;
	private AtomicInteger _comments;

	/**
	 * <b>设置该属性的文章不会被非该空间查询、统计到</b>
	 * </p>
	 */
	private Boolean hidden;

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

	public Article() {
		super();
	}

	public Article(Integer id) {
		super(id);
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
		if (!isPrivate) {
			if (space != null && space.getIsPrivate() != null) {
				return space.getIsPrivate();
			}
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
		return _hits != null ? _hits.get() : hits;
	}

	public void setHits(int hits) {
		this.hits = hits;
		this._hits = new AtomicInteger(hits);
	}

	public int addHits() {
		return _hits.incrementAndGet();
	}

	public int addComments() {
		return _comments.incrementAndGet();
	}

	public int decrementComment(int count) {
		if (count == 1) {
			return _comments.decrementAndGet();
		}
		return _comments.updateAndGet(i -> i - count);
	}

	public int getComments() {
		return _comments != null ? _comments.get() : comments;
	}

	public void setComments(int comments) {
		this.comments = comments;
		this._comments = new AtomicInteger(comments);
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

	public CommentConfig getCommentConfig() {
		return commentConfig;
	}

	public void setCommentConfig(CommentConfig commentConfig) {
		this.commentConfig = commentConfig;
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
			hasLock = (space != null && space.hasLock());
		}
		return hasLock;
	}

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

	@Override
	public Article clone() {
		try {
			return (Article) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new SystemException(e.getMessage(), e);
		}
	}
	
	@Override
	@JsonIgnore
	public String getLockId() {
		return super.getLockId();
	}

}
