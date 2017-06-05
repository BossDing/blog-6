package me.qyh.blog.web.template;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

/**
 * 渲染模板内容
 */
public interface TemplateRenderExecutor {

	/**
	 * 
	 * @param viewTemplateName
	 * @param model
	 *            额外参数
	 * @param request
	 *            当前请求
	 * @param readOnlyResponse
	 *            <b>READ ONLY</b> response
	 * @return
	 * @throws Exception
	 */
	String execute(String viewTemplateName, Map<String, ?> model, HttpServletRequest request,
			ReadOnlyResponse readOnlyResponse) throws Exception;

}
