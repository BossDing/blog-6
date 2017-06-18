package me.qyh.blog.core.vo;

import java.io.Serializable;

import me.qyh.blog.core.entity.Article;

public class ArticleArchiveNode extends Node<ArticleArchiveNode> implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Integer order;
	private Article article;

	public Article getArticle() {
		return article;
	}

	public void setArticle(Article article) {
		this.article = article;
	}

	public Integer getOrder() {
		return order;
	}

	public void setOrder(Integer order) {
		this.order = order;
	}

}
