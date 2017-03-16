package me.qyh.blog.web;

import org.springframework.context.ApplicationEvent;

/**
 * RequestMapping 解除注册事件
 * 
 * @see GetRequestMappingEventListener
 * @author Administrator
 *
 */
public class GetRequestMappingUnRegisterEvent extends ApplicationEvent {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final String path;

	public GetRequestMappingUnRegisterEvent(Object source, String path) {
		super(source);
		this.path = path;
	}

	public String getPath() {
		return path;
	}
}
