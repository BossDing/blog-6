package me.qyh.blog.service;

import me.qyh.blog.config.PageSizeConfig;
import me.qyh.blog.config.UploadConfig;
import me.qyh.blog.entity.CommentConfig;
import me.qyh.blog.exception.LogicException;

public interface ConfigService {

	PageSizeConfig getPageSizeConfig();

	PageSizeConfig updatePageSizeConfig(PageSizeConfig pageSizeConfig);

	CommentConfig getCommentConfig();

	CommentConfig updateCommentConfig(CommentConfig config);

	UploadConfig getMetaweblogConfig();

	UploadConfig updateMetaweblogConfig(UploadConfig uploadConfig) throws LogicException;

}
