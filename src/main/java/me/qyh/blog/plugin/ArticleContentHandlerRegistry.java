package me.qyh.blog.plugin;

import me.qyh.blog.core.service.impl.ArticleContentHandler;

public interface ArticleContentHandlerRegistry {

	ArticleContentHandlerRegistry register(ArticleContentHandler handler);

}
