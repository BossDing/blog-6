package me.qyh.blog.dao;

import me.qyh.blog.config.PageSizeConfig;

public interface PageSizeConfigDao {

	PageSizeConfig select();

	void update(PageSizeConfig pageSizeConfig);

}
