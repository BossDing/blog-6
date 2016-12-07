package me.qyh.blog.ui;

import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.View;
import org.thymeleaf.spring4.view.ThymeleafViewResolver;

public class UIViewResolver extends ThymeleafViewResolver {

	@Autowired
	private UIThymeleafView uiThymeleafView;

	@Override
	protected View loadView(String viewName, Locale locale) throws Exception {
		return super.loadView(viewName, locale);
	}

	@Override
	public View resolveViewName(String viewName, Locale locale) throws Exception {
		RenderedPage page = UIContext.get();
		if (page != null) {
			return uiThymeleafView;
		} else {
			return super.resolveViewName(viewName, locale);
		}
	}

}
