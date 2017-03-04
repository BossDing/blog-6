package me.qyh.blog.evt.listener;

import java.util.ArrayList;
import java.util.List;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

import me.qyh.blog.entity.Article;
import me.qyh.blog.evt.ArticleEvent;

/**
 * 处理文章事件
 * <p>
 * 主要用于处理匿名注入时候EventListener无法触发的时候
 * </p>
 * 
 * <pre>
 * usage:
 * &#64;Autowired
 * private ArticleEventHandler handler
 * 
 * public void afterPropertiesSet(){
 * 	handler.registerEventHandler(handler);
 * }
 * </pre>
 * 
 * @author mhlx
 *
 */
@Component
public class ArticleEventHandlerRegister {

	private List<EventHandler<List<Article>>> eventsHandler = new ArrayList<>();
	private List<EventHandler<List<Article>>> transactionEventsHandler = new ArrayList<>();

	@EventListener
	public void handleEvent(ArticleEvent event) {
		handle(event, eventsHandler);
	}

	@TransactionalEventListener
	public void handleTransactionalEvent(ArticleEvent event) {
		handle(event, transactionEventsHandler);
	}

	private void handle(ArticleEvent event, List<EventHandler<List<Article>>> handlers) {
		for (EventHandler<List<Article>> handler : handlers) {
			switch (event.getEventType()) {
			case INSERT:
				handler.handleInsert(event.getArticles());
				break;
			case UPDATE:
				handler.handleUpdate(event.getArticles());
				break;
			case DELETE:
				handler.handleDelete(event.getArticles());
				break;
			default:
				break;
			}
		}
	}

	/**
	 * 注册文章事件处理器
	 * <p>
	 * <b>支持事务(如果处于事务中)</b>
	 * </p>
	 * 
	 * @param handler
	 */
	public void registerEventHandler(EventHandler<List<Article>> handler) {
		this.eventsHandler.add(handler);
	}

	/**
	 * 注册文章事件处理器
	 * <p>
	 * <b>请勿进行任何事务操作</b>
	 * </p>
	 * 
	 * @param handler
	 */
	public void registerTransactionalEventHandler(EventHandler<List<Article>> handler) {
		this.transactionEventsHandler.add(handler);
	}
}
