package me.qyh.blog.plugin;

import org.springframework.web.servlet.HandlerInterceptor;

public interface HandlerInterceptorRegistry {
	HandlerInterceptorRegistry register(HandlerInterceptor handlerInterceptor);
}
