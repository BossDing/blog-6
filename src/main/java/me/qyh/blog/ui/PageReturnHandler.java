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

import java.io.Writer;
import java.util.HashMap;
import java.util.Objects;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.View;
import org.thymeleaf.spring4.view.ThymeleafViewResolver;

import me.qyh.blog.config.Constants;
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.exception.SystemException;
import me.qyh.blog.message.Message;
import me.qyh.blog.service.UIService;
import me.qyh.blog.ui.page.ErrorPage;
import me.qyh.blog.ui.page.ErrorPage.ErrorCode;
import me.qyh.blog.ui.page.LockPage;
import me.qyh.blog.ui.page.Page;
import me.qyh.blog.ui.page.UserPage;

public class PageReturnHandler implements HandlerMethodReturnValueHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(PageReturnHandler.class);

	@Autowired
	private UIService uiService;
	@Autowired
	private UIRender uiRender;
	@Autowired
	protected ThymeleafViewResolver thymeleafViewResolver;

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

		String templateName = TemplateUtils.getTemplateName(page);

		Page db = uiService.queryPage(templateName);
		
		checkRegistrable(page, db);

		String rendered;

		try {

			rendered = uiRender.doRender(db, mavContainer.getModel(), nativeRequest, nativeResponse,
					ParseContext.DEFAULT_CONFIG);

		} catch (Exception e) {
			// 如果是错误页面发生了错误，不再跳转(防止死循环)
			if ((db instanceof ErrorPage)) {
				ErrorPage errorPage = (ErrorPage) db;
				LOGGER.error("在错误页面" + errorPage.getErrorCode().name() + "发生了一个异常，为了防止死循环，这个页面发生异常将会无法跳转，异常栈信息:"
						+ e.getMessage(), e);
				renderSysErrorPage(errorPage, nativeRequest, nativeResponse);
				return;
			}
			// 解锁页面不能出现异常，不再跳转(防止死循环)
			if (db instanceof LockPage) {
				LockPage lockPage = (LockPage) db;
				LOGGER.error(
						"在解锁页面" + lockPage.getLockType() + "发生了一个异常，为了防止死循环，这个页面发生异常将会无法跳转，异常栈信息:" + e.getMessage(), e);
				renderSysErrorPage(new ErrorPage(ErrorCode.ERROR_500), nativeRequest, nativeResponse);
				return;
			}

			throw e;
		}

		nativeResponse.setContentLength(rendered.getBytes(Constants.CHARSET).length);
		Writer writer = nativeResponse.getWriter();
		writer.write(rendered);
		writer.flush();
	}

	private void checkRegistrable(Page returnValue, Page db) throws LogicException {
		if (returnValue instanceof UserPage) {
			UserPage returnUserPage = (UserPage) returnValue;
			UserPage dbUserPage = (UserPage) db;
			if (!Objects.equals(returnUserPage.isRegistrable(), dbUserPage.isRegistrable())) {
				throw new LogicException(new Message("page.user.notExists", "自定义页面不存在"));
			}
		}
	}

	private void renderSysErrorPage(ErrorPage errorPage, HttpServletRequest request, HttpServletResponse response) {
		try {
			// render /WEB-INF/templates/error/{errorCode}
			View errorView = thymeleafViewResolver.resolveViewName("error/" + errorPage.getErrorCode().getCode(),
					request.getLocale());
			errorView.render(new HashMap<>(), request, response);
		} catch (Throwable e) {
			// 不能够在这里继续抛出异常!
			LOGGER.error("/WEB-INF/templates/error/" + errorPage.getErrorCode().name() + "页面渲染异常！！！！！,异常信息:"
					+ e.getMessage(), e);
		}
	}
}
