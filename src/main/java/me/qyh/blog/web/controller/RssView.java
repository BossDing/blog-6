package me.qyh.blog.web.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.view.feed.AbstractRssFeedView;

import com.sun.syndication.feed.rss.Channel;
import com.sun.syndication.feed.rss.Content;
import com.sun.syndication.feed.rss.Item;

import me.qyh.blog.config.Constants;
import me.qyh.blog.config.UrlHelper;
import me.qyh.blog.entity.Article;
import me.qyh.blog.entity.Space;
import me.qyh.blog.pageparam.PageResult;
import me.qyh.blog.web.interceptor.SpaceContext;

@Component
public class RssView extends AbstractRssFeedView {

	@Autowired
	private UrlHelper urlHelper;

	@Override
	protected List<Item> buildFeedItems(Map<String, Object> model, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		@SuppressWarnings("unchecked")
		PageResult<Article> page = (PageResult<Article>) model.get("page");
		List<Item> items = new ArrayList<Item>();
		if (page.hasResult()) {
			for (Article article : page.getDatas()) {
				Item item = new Item();
				Content content = new Content();
				content.setValue(article.getSummary());
				item.setContent(content);
				item.setTitle(article.getTitle());
				item.setLink(urlHelper.getUrls().getUrl(article));
				item.setPubDate(article.getPubDate());
				items.add(item);
			}
		}
		return items;
	}

	@Override
	protected void buildFeedMetadata(Map<String, Object> model, Channel feed, HttpServletRequest request) {
		Space space = SpaceContext.get();
		if (space == null) {
			feed.setLink(urlHelper.getUrl());
			String domain = urlHelper.getUrlConfig().getDomain();
			feed.setDescription(domain);
			feed.setTitle(domain);
		} else {
			feed.setTitle(space.getName());
			feed.setDescription(space.getName());
			feed.setLink(urlHelper.getUrls().getUrl(space));
		}
		feed.setEncoding(Constants.CHARSET.name());
	}

}
