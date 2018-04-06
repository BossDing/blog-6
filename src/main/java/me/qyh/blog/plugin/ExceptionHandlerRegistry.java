package me.qyh.blog.plugin;

import me.qyh.blog.web.ExceptionHandler;

public interface ExceptionHandlerRegistry {

	ExceptionHandlerRegistry register(ExceptionHandler exceptionHandler);

}
