package me.qyh.blog.evt;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.ApplicationListener;

import me.qyh.blog.evt.handler.CommentHandler;

public class CommentEventListener implements ApplicationListener<CommentEvent> {

	private List<CommentHandler> handlers = new ArrayList<CommentHandler>();

	@Override
	public final void onApplicationEvent(CommentEvent event) {
		for (CommentHandler handle : handlers)
			handle.handle(event.getComment());
	}

	public void setHandlers(List<CommentHandler> handlers) {
		this.handlers = handlers;
	}

}
