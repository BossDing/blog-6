package me.qyh.blog.bean;

import me.qyh.blog.entity.Article;

public class ArticleNav {

	private Article previous;
	private Article next;

	public Article getPrevious() {
		return previous;
	}

	public void setPrevious(Article previous) {
		this.previous = previous;
	}

	public Article getNext() {
		return next;
	}

	public void setNext(Article next) {
		this.next = next;
	}

	public ArticleNav(Article previous, Article next) {
		this.previous = previous;
		this.next = next;
	}

}
