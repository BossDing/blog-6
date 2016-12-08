/*
 * Copyright 2016 qyh.me
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package me.qyh.blog.ui;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.i18n.LocaleContextHolder;
import org.thymeleaf.spring4.view.ThymeleafView;
import org.thymeleaf.util.FastStringWriter;

import me.qyh.blog.config.Constants;

public class UIThymeleafView extends ThymeleafView implements InitializingBean {

	@Autowired
	private UITemplateEngine templateEngine;

	@Override
	public String getContentType() {
		return "text/html;charset=" + Constants.CHARSET.name();
	}

	@Override
	public String getTemplateName() {
		return UIContext.get().getTemplateName();
	}

	@Override
	protected Locale getLocale() {
		return LocaleContextHolder.getLocale();
	}

	@Override
	protected void renderFragment(Set<String> markupSelectorsToRender, Map<String, ?> model, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		try {
			ResponseWrapper wrapper = new ResponseWrapper(response);
			super.renderFragment(markupSelectorsToRender, model, request, wrapper);
			PrintWriter writer = response.getWriter();
			writer.write(wrapper.getRendered());
			writer.flush();
		} catch (Throwable e) {
			throw new TplRenderException(TplRenderExceptionHandler.getHandler().convert(e, getServletContext()), e);
		}
	}

	public static final class ResponseWrapper extends HttpServletResponseWrapper {

		private FastStringWriter writer = new FastStringWriter(100);

		public ResponseWrapper(HttpServletResponse response) {
			super(response);
		}

		@Override
		public PrintWriter getWriter() throws IOException {
			return new PrintWriter(writer);
		}

		public String getRendered() {
			return writer.toString();
		}

	}

	@Override
	public void afterPropertiesSet() throws Exception {
		setTemplateEngine(templateEngine);
	}

}
