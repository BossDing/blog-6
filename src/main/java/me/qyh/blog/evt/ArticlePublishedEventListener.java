package me.qyh.blog.evt;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.ApplicationListener;

import me.qyh.blog.evt.handler.ArticlePublishedHandler;

public class ArticlePublishedEventListener implements ApplicationListener<ArticlePublishedEvent> {

	private List<ArticlePublishedHandler> handlers = new ArrayList<ArticlePublishedHandler>();

	@Override
	public final void onApplicationEvent(ArticlePublishedEvent event) {
		for (ArticlePublishedHandler handle : handlers)
			handle.handle(event.getArticles(), event.getOp());
	}

	public void setHandlers(List<ArticlePublishedHandler> handlers) {
		this.handlers = handlers;
	}

}
