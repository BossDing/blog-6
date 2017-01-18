package me.qyh.blog.evt;

import org.springframework.context.ApplicationEvent;

/**
 * 文章索引重建事件
 * 
 * @author Administrator
 *
 */
public class ArticleIndexRebuildEvent extends ApplicationEvent {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public ArticleIndexRebuildEvent(Object source) {
		super(source);
	}

}
