package me.qyh.blog.ui;

import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.thymeleaf.spring4.view.ThymeleafView;

public class _ThymeleafView extends ThymeleafView {

	@Override
	protected void renderFragment(Set<String> markupSelectorsToRender, Map<String, ?> model, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		try {
			super.renderFragment(markupSelectorsToRender, model, request, response);
		} catch (Throwable e) {
			throw new TplRenderException(TplRenderExceptionHandler.getHandler().convert(e, getServletContext()), e);
		}
	}

}
