package me.qyh.blog.evt;

import java.util.Arrays;
import java.util.List;

import org.springframework.context.ApplicationEvent;

import me.qyh.blog.entity.Article;

public class ArticlePublishedEvent extends ApplicationEvent {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private List<Article> articles;
	private OP op;

	public enum OP {
		INSERT, UPDATE;
	}

	public ArticlePublishedEvent(Object source, Article article, OP op) {
		super(source);
		this.articles = Arrays.asList(article);
		this.op = op;
	}

	public ArticlePublishedEvent(Object source, List<Article> articles, OP op) {
		super(source);
		this.articles = articles;
		this.op = op;
	}

	public List<Article> getArticles() {
		return articles;
	}

	public OP getOp() {
		return op;
	}

}
