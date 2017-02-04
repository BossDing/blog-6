package me.qyh.blog.service.impl;

import java.util.List;

import org.springframework.util.CollectionUtils;

import com.google.common.collect.Lists;

import me.qyh.blog.entity.Article;

public class ArticleContentHandlers implements ArticleContentHandler {

	private List<ArticleContentHandler> handlers = Lists.newArrayList();

	@Override
	public void handle(Article article) {
		if (!CollectionUtils.isEmpty(handlers)) {
			for (ArticleContentHandler handler : handlers) {
				handler.handle(article);
			}
		}
	}

	@Override
	public void handlePreview(Article article) {
		if (!CollectionUtils.isEmpty(handlers)) {
			for (ArticleContentHandler handler : handlers) {
				handler.handlePreview(article);
			}
		}
	}

	public void setHandlers(List<ArticleContentHandler> handlers) {
		this.handlers = handlers;
	}

}
