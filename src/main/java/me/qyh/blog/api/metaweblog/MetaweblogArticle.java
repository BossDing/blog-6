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
package me.qyh.blog.api.metaweblog;

import java.sql.Timestamp;

import me.qyh.blog.entity.Article;
import me.qyh.blog.entity.Editor;
import me.qyh.blog.entity.Article.ArticleFrom;
import me.qyh.blog.entity.Article.ArticleStatus;

/**
 * metaweblog api撰写的文章
 * 
 * @author Administrator
 *
 */
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

	/**
	 * 设置文章内容
	 * 
	 * @param ori
	 */
	public void mergeArticle(Article ori) {
		ori.setTitle(title);
		ori.setContent(content);
		ori.setPubDate(pubDate);
		ori.setStatus(status);
	}

	/**
	 * 转化为系统文章
	 * 
	 * @return
	 */
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
		article.setAllowComment(true);
		article.setContent(content);
		return article;
	}

	/**
	 * 是否包含id
	 * 
	 * @return 是true，否false
	 */
	public boolean hasId() {
		return id != null;
	}

}
