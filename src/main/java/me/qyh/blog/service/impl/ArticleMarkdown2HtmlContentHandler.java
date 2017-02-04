package me.qyh.blog.service.impl;

import java.util.stream.Collectors;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.annotation.Async;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import me.qyh.blog.entity.Article;
import me.qyh.blog.entity.Editor;
import me.qyh.blog.evt.ArticleEvent;
import me.qyh.blog.evt.EventType;
import me.qyh.blog.security.input.Markdown2Html;

/**
 * 将文章的markdown转化为html内容
 * 
 * @author mhlx
 *
 */
public class ArticleMarkdown2HtmlContentHandler
		implements ArticleContentHandler, ApplicationListener<ArticleEvent>, InitializingBean {

	private static final String DEFAULT_CACHESPECIFICATION = "maximumSize=500";
	private String cacheSpecification = DEFAULT_CACHESPECIFICATION;

	@Autowired
	private Markdown2Html markdown2Html;

	private Cache<Integer, String> markdownCache;

	@Override
	public void handle(Article article) {
		if (Editor.MD.equals(article.getEditor())) {
			String cached = markdownCache.getIfPresent(article.getId());
			if (cached == null) {
				cached = markdown2Html.toHtml(article.getContent());
				markdownCache.put(article.getId(), cached);
			}
			article.setContent(cached);
		}
	}

	@Override
	public void handlePreview(Article article) {
		if (Editor.MD.equals(article.getEditor())) {
			article.setContent(markdown2Html.toHtml(article.getContent()));
		}
	}

	@Async
	@Override
	public void onApplicationEvent(ArticleEvent event) {
		EventType eventType = event.getEventType();
		if (!EventType.INSERT.equals(eventType)) {
			markdownCache.invalidateAll(event.getArticles().stream().map(Article::getId).collect(Collectors.toList()));
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (cacheSpecification != null) {
			markdownCache = CacheBuilder.from(cacheSpecification).build();
		} else {
			markdownCache = CacheBuilder.newBuilder().build();
		}
	}

	public void setCacheSpecification(String cacheSpecification) {
		this.cacheSpecification = cacheSpecification;
	}

}
