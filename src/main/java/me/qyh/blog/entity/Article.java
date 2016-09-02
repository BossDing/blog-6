package me.qyh.blog.entity;

import java.sql.Timestamp;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.annotation.JsonFormat;

import me.qyh.blog.message.Message;

public class Article extends BaseLockResource {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private Space space;// 空间
	private String title;// 标题
	private String content;// 博客原始内容
	private Set<Tag> tags = new LinkedHashSet<Tag>();// 博客标签
	@JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
	private Timestamp pubDate;// 撰写日期
	private Timestamp lastModifyDate;// 最后修改日期
	private Boolean isPrivate;// 是否是私人博客
	private int hits;// 点击数量
	private int comments;// 评论数量
	private ArticleFrom from;// 博客来源
	private ArticleStatus status;// 博客状态
	private Editor editor;// 编辑器
	private String summary;// 博客摘要
	private Boolean allowComment;// 是否允许评论
	private Integer level; // 博客级别，级别越高显示越靠前
	public CommentMode commentMode;

	private AtomicInteger _hits;
	private AtomicInteger _comments;

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
		return _comments.updateAndGet(i -> i > count ? i - count : 0);
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

	public Boolean getAllowComment() {
		return allowComment;
	}

	public void setAllowComment(Boolean allowComment) {
		this.allowComment = allowComment;
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

	public boolean isCacheable() {
		return isPublished() && !isPrivate;
	}

	public boolean isPublished() {
		return ArticleStatus.PUBLISHED.equals(status);
	}

	public boolean isDraft() {
		return ArticleStatus.DRAFT.equals(status);
	}

	@Override
	public Message getLockTip() {
		return new Message("lock.article.tip", "该文章访问受密码保护，请解锁后访问");
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public CommentMode getCommentMode() {
		return commentMode;
	}

	public void setCommentMode(CommentMode commentMode) {
		this.commentMode = commentMode;
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
}
