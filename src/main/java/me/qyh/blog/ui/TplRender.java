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
import org.thymeleaf.spring4.view.ThymeleafViewResolver;
import org.thymeleaf.util.FastStringWriter;

import me.qyh.blog.exception.SystemException;

/**
 * 用来校验用户的自定义模板<br/>
 * 
 * @author Administrator
 *
 */
public class TplRender {

	@Autowired
	private UIExposeHelper uiExposeHelper;
	@Autowired
	private ThymeleafViewResolver resolver;

	public String tryRender(RenderedPage page, HttpServletRequest request, HttpServletResponse response)
			throws TplRenderException {
		UIContext.set(page);
		return doRender(page.getTemplateName(), request, response, page.getDatas());
	}

	/**
	 */
	private String doRender(String viewTemplateName, HttpServletRequest request, HttpServletResponse response,
			Map<String, Object> datas) throws TplRenderException {
		// 清除模板缓存
		try {
			if (datas == null) {
				datas = new HashMap<String, Object>();
			}
			datas.putAll(uiExposeHelper.getHelpers(request));
			View view = resolver.resolveViewName(viewTemplateName, request.getLocale());
			// 调用view来渲染模板，获取response中的数据
			TemplateDebugResponseWrapper wrapper = new TemplateDebugResponseWrapper(response);
			view.render(datas, request, wrapper);
			// 再次清除缓存
			return wrapper.output();
		} catch (Exception e) {
			if (e instanceof TplRenderException)
				throw (TplRenderException) e;
			throw new SystemException(e.getMessage(), e);
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

}
