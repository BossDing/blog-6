package me.qyh.blog.web.view;

import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.AbstractCachingViewResolver;
import org.springframework.web.servlet.view.InternalResourceView;
import org.springframework.web.servlet.view.RedirectView;

import me.qyh.blog.core.Constants;
import me.qyh.blog.web.template.render.ParseConfig;
import me.qyh.blog.web.template.render.ReadOnlyResponse;
import me.qyh.blog.web.template.render.RenderResult;
import me.qyh.blog.web.template.render.TemplateRender;


@Component
public class TemplateViewResolver extends AbstractCachingViewResolver {

	public static final String REDIRECT_URL_PREFIX = "redirect:";
	public static final String FORWARD_URL_PREFIX = "forward:";

	@Autowired
	private TemplateRender templateRender;

	@Override
	protected View createView(final String viewName, final Locale locale) throws Exception {
		if (viewName.startsWith(REDIRECT_URL_PREFIX)) {
			final String redirectUrl = viewName.substring(REDIRECT_URL_PREFIX.length(), viewName.length());
			final RedirectView view = new RedirectView(redirectUrl, true, true);
			return (View) getApplicationContext().getAutowireCapableBeanFactory().initializeBean(view, viewName);
		}
		if (viewName.startsWith(FORWARD_URL_PREFIX)) {
			final String forwardUrl = viewName.substring(FORWARD_URL_PREFIX.length(), viewName.length());
			return new InternalResourceView(forwardUrl);
		}
		return loadView(viewName, locale);
	}

	@Override
	protected View loadView(String viewName, Locale locale) throws Exception {
		return new _View(viewName);
	}

	private final class _View implements View {

		private final String templateName;

		public _View(String templateName) {
			super();
			this.templateName = templateName;
		}

		@Override
		public String getContentType() {
			return "text/html";
		}

		@Override
		public void render(Map<String, ?> model, HttpServletRequest request, HttpServletResponse response)
				throws Exception {
			RenderResult rendered = templateRender.doRender(templateName, model, request,
					new ReadOnlyResponse(response), new ParseConfig());

			response.setContentType(getContentType());
			response.setCharacterEncoding(Constants.CHARSET.name());
			response.getWriter().write(rendered.getContent());
			response.getWriter().flush();

		}

	}

}
