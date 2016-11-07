package me.qyh.blog.metaweblog;

import java.sql.Timestamp;

import me.qyh.blog.entity.Article;
import me.qyh.blog.entity.Editor;
import me.qyh.blog.entity.Article.ArticleFrom;
import me.qyh.blog.entity.Article.ArticleStatus;

public class MetaweblogArticle {

	private String space;
	private String title;
	private String content;
	private Timestamp pubDate;
	private ArticleStatus status;

	private Integer id;

	public String getSpace() {
		return space;
	}

	public void setSpace(String space) {
		this.space = space;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public Timestamp getPubDate() {
		return pubDate;
	}

	public void setPubDate(Timestamp pubDate) {
		this.pubDate = pubDate;
	}

	public ArticleStatus getStatus() {
		return status;
	}

	public void setStatus(ArticleStatus status) {
		this.status = status;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public void mergeArticle(Article ori) {
		ori.setTitle(title);
		ori.setContent(content);
		ori.setPubDate(pubDate);
		ori.setStatus(status);
	}

	public Article toArticle() {
		Article article = new Article();
		article.setTitle(title);
		article.setStatus(status);
		article.setPubDate(pubDate);
		article.setStatus(status);
		article.setIsPrivate(false);
		article.setEditor(Editor.HTML);
		article.setFrom(ArticleFrom.ORIGINAL);
		article.setSummary("");
		article.setContent(content);
		return article;
	}

	public boolean hasId() {
		return id != null;
	}

}
