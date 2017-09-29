package me.qyh.blog.template.render;

import javax.servlet.http.HttpServletRequest;

public interface TemplateRenderHandler {

	boolean match(String templateName);

	void afterRender(RenderResult result, HttpServletRequest request);

}
