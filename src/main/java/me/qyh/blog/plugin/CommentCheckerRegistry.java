package me.qyh.blog.plugin;

import me.qyh.blog.comment.service.CommentChecker;

public interface CommentCheckerRegistry {

	CommentCheckerRegistry registry(CommentChecker commentChecker);

}
