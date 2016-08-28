package me.qyh.blog.ui;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.View;
import org.thymeleaf.engine.TemplateManager;
import org.thymeleaf.spring4.view.ThymeleafViewResolver;
import org.thymeleaf.util.FastStringWriter;

import me.qyh.blog.config.UrlHelper;
import me.qyh.blog.message.Messages;

/**
 * 用来校验用户的自定义模板<br/>
 * 
 * @author Administrator
 *
 */
public class TplRender {

	@Autowired
	private UrlHelper urlHelper;
	@Autowired
	private Messages messages;
	@Autowired
	private TplRenderExceptionHandler tplRenderExceptionHandler;
	@Autowired
	private ThymeleafViewResolver resolver;

	public String tryRender(Template template, HttpServletRequest request, HttpServletResponse response)
			throws TplRenderException {
		UIContext.set(template);
		return doRender(template.getTemplateName(), request, response, template.getTemplateDatas());
	}

	/**
	 */
	private String doRender(String viewTemplateName, HttpServletRequest request, HttpServletResponse response,
			Map<String, Object> datas) throws TplRenderException {
		// 清除模板缓存
		TemplateManager templateManager = resolver.getTemplateEngine().getConfiguration().getTemplateManager();
		templateManager.clearCachesFor(viewTemplateName);
		try {
			if (datas == null) {
				datas = new HashMap<String, Object>();
			}
			datas.put("urls", urlHelper.getUrls(request));
			datas.put("messages", messages);
			View view = resolver.resolveViewName(viewTemplateName, request.getLocale());
			// 调用view来渲染模板，获取response中的数据
			TemplateDebugResponseWrapper wrapper = new TemplateDebugResponseWrapper(response);
			view.render(datas, request, wrapper);
			// 再次清除缓存
			templateManager.clearCachesFor(viewTemplateName);
			return wrapper.output();
		} catch (Throwable e) {
			throw tplRenderExceptionHandler.convertThrowable(e);
		}
	}

	private final class TemplateDebugResponseWrapper extends HttpServletResponseWrapper {

		private TemplateDebugResponseWrapper(HttpServletResponse response) {
			super(response);
		}

		private FastStringWriter writer = new FastStringWriter(100);

		public PrintWriter getWriter() throws IOException {
			return new PrintWriter(writer);
		}

		public String output() {
			return writer.toString();
		}
	}

	/**
	 * 用来描述错误，向用户反馈
	 * 
	 * @author Administrator
	 *
	 */
	public interface TplRenderExceptionHandler {
		TplRenderException convertThrowable(Throwable throwable);
	}

	public void setTplRenderExceptionHandler(TplRenderExceptionHandler tplRenderExceptionHandler) {
		this.tplRenderExceptionHandler = tplRenderExceptionHandler;
	}

}
