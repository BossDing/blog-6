package me.qyh.blog.core.plugin;

import me.qyh.blog.web.LogoutHandler;

public interface LogoutHandlerRegistry {
	LogoutHandlerRegistry register(LogoutHandler handler);
}
