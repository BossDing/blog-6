package me.qyh.blog.evt.handler;

import java.util.List;

import me.qyh.blog.entity.Article;
import me.qyh.blog.evt.ArticlePublishedEvent.OP;

public interface ArticlePublishedHandler {

	void handle(List<Article> articles, OP op);

}
