package me.qyh.blog.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.commonmark.Extension;
import org.commonmark.ext.autolink.AutolinkExtension;
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.ext.heading.anchor.HeadingAnchorExtension;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.util.CollectionUtils;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

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

	private List<Extension> extensions = Lists.newArrayList();

	/**
	 * singleton?
	 */
	private Parser parser;
	private HtmlRenderer renderer;

	private static final List<Extension> BASE_EXTENSIONS = ImmutableList.of(AutolinkExtension.create(),
			TablesExtension.create(), StrikethroughExtension.create(), HeadingAnchorExtension.create());

	public void setExtensions(List<Extension> extensions) {
		this.extensions = extensions;
	}

	private Cache<Integer, String> markdownCache;

	@Override
	public void handle(Article article) {
		if (Editor.MD.equals(article.getEditor())) {
			String cached = markdownCache.getIfPresent(article.getId());
			if (cached == null) {
				cached = toHtml(article.getContent());
				markdownCache.put(article.getId(), cached);
			}
			article.setContent(cached);
		}
	}

	@Override
	public void handlePreview(Article article) {
		if (Editor.MD.equals(article.getEditor())) {
			article.setContent(toHtml(article.getContent()));
		}
	}

	@Async
	@Override
	public void onApplicationEvent(ArticleEvent event) {
		EventType eventType = event.getEventType();
		if (!EventType.HITS.equals(eventType) && !EventType.INSERT.equals(eventType)) {
			markdownCache.invalidateAll(event.getArticles().stream().map(Article::getId).collect(Collectors.toList()));
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		List<Extension> extensions = Lists.newArrayList(BASE_EXTENSIONS);
		if (!CollectionUtils.isEmpty(this.extensions)) {
			extensions.addAll(this.extensions);
		}
		parser = Parser.builder().extensions(extensions).build();
		renderer = HtmlRenderer.builder().extensions(extensions).build();

		if (cacheSpecification != null) {
			markdownCache = CacheBuilder.from(cacheSpecification).build();
		} else {
			markdownCache = CacheBuilder.newBuilder().build();
		}
	}

	public void setCacheSpecification(String cacheSpecification) {
		this.cacheSpecification = cacheSpecification;
	}

	private String toHtml(String markdown) {
		if (markdown == null) {
			return "";
		}
		Node document = parser.parse(markdown);
		return renderer.render(document);
	}

}
