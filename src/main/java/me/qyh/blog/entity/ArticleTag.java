package me.qyh.blog.entity;

public class ArticleTag {

	private Article article;
	private Tag tag;

	public Article getArticle() {
		return article;
	}

	public void setArticle(Article article) {
		this.article = article;
	}

	public Tag getTag() {
		return tag;
	}

	public void setTag(Tag tag) {
		this.tag = tag;
	}

	public ArticleTag() {

	}

	public ArticleTag(Article article, Tag tag) {
		this.article = article;
		this.tag = tag;
	}

}
