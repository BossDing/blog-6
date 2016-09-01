package me.qyh.blog.service;

import me.qyh.blog.config.CommentConfig;
import me.qyh.blog.config.PageSizeConfig;

public interface ConfigService {

	PageSizeConfig getPageSizeConfig();

	PageSizeConfig updatePageSizeConfig(PageSizeConfig pageSizeConfig);

	CommentConfig getCommentConfig();

	CommentConfig updateCommentConfig(CommentConfig config);

}
