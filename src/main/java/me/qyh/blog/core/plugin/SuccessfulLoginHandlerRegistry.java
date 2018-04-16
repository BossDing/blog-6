package me.qyh.blog.core.plugin;

import me.qyh.blog.web.SuccessfulLoginHandler;

public interface SuccessfulLoginHandlerRegistry {
	SuccessfulLoginHandlerRegistry registry(SuccessfulLoginHandler handler);
}
