package me.qyh.blog.service.impl;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.annotation.Async;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import me.qyh.blog.entity.Article;
import me.qyh.blog.entity.Editor;
import me.qyh.blog.evt.ArticleEvent;
import me.qyh.blog.evt.ArticleEvent.EventType;
import me.qyh.blog.service.impl.ArticleServiceImpl.ArticleContentHandler;

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

	private LoadingCache<Article, String> markdownCache;

	@Autowired
	private Markdown2Html markdown2Html;

	@Override
	public void handle(Article article) {
		if (Editor.MD.equals(article.getEditor())) {
			String html = markdownCache.getUnchecked(article);
			article.setContent(html);
		}
	}

	@Async
	@Override
	public void onApplicationEvent(ArticleEvent event) {
		EventType eventType = event.getEventType();
		if (!EventType.HITS.equals(eventType) && !EventType.INSERT.equals(eventType)) {
			markdownCache.invalidateAll(event.getArticles());
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		CacheLoader<Article, String> markdownCacheLoader = new CacheLoader<Article, String>() {

			@Override
			public String load(Article article) throws Exception {
				return markdown2Html.toHtml(article.getContent());
			}

		};
		if (cacheSpecification != null) {
			markdownCache = CacheBuilder.from(cacheSpecification).build(markdownCacheLoader);
		} else {
			markdownCache = CacheBuilder.newBuilder().build(markdownCacheLoader);
		}
	}

	public void setCacheSpecification(String cacheSpecification) {
		this.cacheSpecification = cacheSpecification;
	}

}
