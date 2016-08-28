package me.qyh.blog.ui;

import me.qyh.blog.exception.LogicException;
import me.qyh.blog.ui.widget.WidgetTag;
import me.qyh.blog.ui.widget.WidgetTpl;

/**
 * Page tpl解析器
 * 
 * @author Administrator
 *
 */
public interface TemplateParser {

	public interface WidgetQuery {
		/**
		 * 根据用户的widget标签查询对应的widgetTpl
		 * 
		 * @param widgetTag
		 *            widget标签，不会为null
		 * @return
		 * @throws LogicException
		 * @throws MissParamException
		 */
		WidgetTpl query(WidgetTag widgetTag) throws LogicException;

	}

	/**
	 * 解析页面的组织模板，将其中Widget标签填充为Widget的tpl
	 * 
	 * @param tpl
	 * @param query
	 * @return 不会为null
	 * @throws LogicException
	 * @throws MissParamException
	 */
	ParseResult parse(String tpl, WidgetQuery query) throws LogicException;
}
