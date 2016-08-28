package me.qyh.blog.service;

import me.qyh.blog.config.PageSizeConfig;

public interface ConfigService {

	PageSizeConfig getPageSizeConfig();

	void updatePageSizeConfig(PageSizeConfig pageSizeConfig);

}
