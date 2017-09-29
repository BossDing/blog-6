package me.qyh.blog.comment.vo;

import me.qyh.blog.comment.entity.Comment;
import me.qyh.blog.core.entity.Article;

/**
 * 最近的文章评论
 * <p>在查询最近的文章评论的时候，如果文章是受保护的，那么评论也应该是受保护的</p>
 */
public class LastArticleComment extends Comment{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private Article article;

	public Article getArticle() {
		return article;
	}

	public void setArticle(Article article) {
		this.article = article;
	}
	
}
