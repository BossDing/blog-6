package me.qyh.blog.web;

import org.springframework.context.ApplicationEvent;

/**
 * 
 * @see AppSimpleUrlHandlerMapping
 * @author Administrator
 *
 */
public class SimpleUrlMappingRegisterEvent extends ApplicationEvent {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final String path;
	private final Object handler;

	public SimpleUrlMappingRegisterEvent(Object source, String path, Object handler) {
		super(source);
		this.path = path;
		this.handler = handler;
	}

	public String getPath() {
		return path;
	}

	public Object getHandler() {
		return handler;
	}

}
