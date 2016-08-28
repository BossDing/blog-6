package me.qyh.blog.dao;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import me.qyh.blog.ui.page.Page;
import me.qyh.blog.ui.widget.Widget;
import me.qyh.blog.ui.widget.WidgetTpl;

public interface WidgetTplDao {

	void insert(WidgetTpl tpl);

	WidgetTpl selectByPageAndWidget(@Param("page") Page page, @Param("widget") Widget widget);

	void update(WidgetTpl tpl);

	List<WidgetTpl> selectByWidget(Widget widget);

	List<WidgetTpl> selectByPage(Page page);

	void deleteById(Integer id);

}
