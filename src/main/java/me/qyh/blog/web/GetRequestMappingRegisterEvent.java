package me.qyh.blog.web;

import java.lang.reflect.Method;

import org.springframework.context.ApplicationEvent;

/**
 * RequestMapping 注册事件
 * <p>
 * <b>只能注册一个GET类型的请求，相当于注册了如下RequestMapping</b>
 * 
 * <pre>
 * PatternsRequestCondition prc = new PatternsRequestCondition(registPath);
 * RequestMethodsRequestCondition rmrc = new RequestMethodsRequestCondition(RequestMethod.GET);
 * return new RequestMappingInfo(prc, rmrc, null, null, null, null, null);
 * </pre>
 * </p>
 * <p>
 * <b>这个事件可以在root Application中发布</b>
 * </p>
 * 
 * @see GetRequestMappingEventListener
 * @author Administrator
 *
 */
public class GetRequestMappingRegisterEvent extends ApplicationEvent {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final String path;
	private final Object handler;
	private final Method method;

	public GetRequestMappingRegisterEvent(Object source, String path, Object handler, Method method) {
		super(source);
		this.path = path;
		this.handler = handler;
		this.method = method;
	}

	public String getPath() {
		return path;
	}

	public Object getHandler() {
		return handler;
	}

	public Method getMethod() {
		return method;
	}

}
