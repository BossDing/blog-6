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
package me.qyh.blog.core.ui;

import java.io.Writer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.ModelAndViewContainer;

import me.qyh.blog.core.config.Constants;
import me.qyh.blog.core.exception.SystemException;
import me.qyh.blog.core.ui.page.LockPage;
import me.qyh.blog.core.ui.page.Page;

public class PageReturnHandler implements HandlerMethodReturnValueHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(PageReturnHandler.class);

	@Autowired
	private TemplateRender uiRender;

	@Override
	public boolean supportsReturnType(MethodParameter returnType) {
		return Page.class.isAssignableFrom(returnType.getParameterType());
	}

	@Override
	public void handleReturnValue(Object returnValue, MethodParameter returnType, ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest) throws Exception {
		mavContainer.setRequestHandled(true);
		HttpServletResponse nativeResponse = webRequest.getNativeResponse(HttpServletResponse.class);
		HttpServletRequest nativeRequest = webRequest.getNativeRequest(HttpServletRequest.class);

		Page page = (Page) returnValue;
		if (page == null) {
			throw new SystemException("不应该返回空页面");
		}
		if (Page.class.equals(page.getClass())) {
			throw new SystemException("必须返回一个确切的页面");
		}

		String templateName = page.getTemplateName();

		String rendered;

		try {

			rendered = uiRender.doRender(templateName, mavContainer.getModel(), nativeRequest, nativeResponse,
					new ParseConfig());

		} catch (Exception e) {

			Page fromTemplateName = TemplateUtils.toPage(templateName);
			// 解锁页面不能出现异常，不再跳转(防止死循环)
			if (fromTemplateName instanceof LockPage) {
				LockPage lockPage = (LockPage) fromTemplateName;
				LOGGER.error(
						"在解锁页面" + lockPage.getLockType() + "发生了一个异常，为了防止死循环，这个页面发生异常将会无法跳转，异常栈信息:" + e.getMessage(), e);
				return;
			}

			throw e;
		}

		nativeResponse.setContentLength(rendered.getBytes(Constants.CHARSET).length);
		nativeResponse.setContentType("text/html");
		nativeResponse.setCharacterEncoding(Constants.CHARSET.name());

		Writer writer = nativeResponse.getWriter();
		writer.write(rendered);
		writer.flush();
	}
}
