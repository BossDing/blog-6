package me.qyh.blog.plugin;

import me.qyh.blog.comment.module.CommentModuleHandler;

public interface CommentModuleHandlerRegistry {

	CommentModuleHandlerRegistry register(CommentModuleHandler commentModuleHandler);

}
