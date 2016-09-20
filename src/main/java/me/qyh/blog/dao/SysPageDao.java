package me.qyh.blog.dao;

import org.apache.ibatis.annotations.Param;

import me.qyh.blog.entity.Space;
import me.qyh.blog.ui.page.Page;
import me.qyh.blog.ui.page.SysPage;
import me.qyh.blog.ui.page.SysPage.PageTarget;

public interface SysPageDao {

	SysPage selectBySpaceAndPageTarget(@Param("space") Space space, @Param("pageTarget") PageTarget target);

	void insert(SysPage sysPage);

	void update(SysPage sysPage);

	void deleteById(Integer id);
	
	Page selectById(Integer id);

}
