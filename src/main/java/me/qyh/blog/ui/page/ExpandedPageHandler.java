package me.qyh.blog.ui.page;

import javax.servlet.http.HttpServletRequest;

import me.qyh.blog.ui.Params;

public interface ExpandedPageHandler {

	/**
	 * 是否能够处理这个请求
	 * 
	 * @param request
	 * @return
	 */
	boolean match(HttpServletRequest request);

	Params fromHttpRequest(HttpServletRequest request);

	/**
	 * 获取页面模板
	 * 
	 * @return
	 */
	String getTemplate();

	/**
	 * 拓展页面的唯一ID
	 * 
	 * @return
	 */
	int id();

	/**
	 * 拓展页面名称(用来在页面显示)
	 * 
	 * @return
	 */
	String name();
}
