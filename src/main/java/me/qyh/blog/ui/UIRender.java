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
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.web.servlet.View;
import org.thymeleaf.spring4.view.ThymeleafViewResolver;
import org.thymeleaf.util.FastStringWriter;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Maps;

import me.qyh.blog.exception.SystemException;
import me.qyh.blog.security.Environment;
import me.qyh.blog.service.SpaceService;
import me.qyh.blog.ui.ParseContext.ParseConfig;
import me.qyh.blog.ui.page.Page;

/**
 * 用来将模板解析成字符串
 * 
 * @author Administrator
 *
 */
public final class UIRender {

	private static final Logger TIME_LOGGER = LoggerFactory.getLogger(UIRender.class);

	@Autowired
	private SpaceService spaceService;
	@Autowired
	protected ThymeleafViewResolver thymeleafViewResolver;
	@Autowired
	private UIExposeHelper uiExposeHelper;
	@Autowired
	private PlatformTransactionManager transactionManager;

	public String render(Page page, Map<String, Object> model, HttpServletRequest request, HttpServletResponse response,
			ParseConfig config) throws TplRenderException {
		// set space
		if (!Environment.hasSpace() && page.getSpace() != null) {
			Environment.setSpace(spaceService.getSpace(page.getSpace().getId())
					.orElseThrow(() -> new SystemException("空间:" + page.getSpace() + "不存在")));
		}
		try {
			return doRender(page, model == null ? Maps.newHashMap() : model, request, response, config);
		} catch (TplRenderException e) {
			throw e;
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new SystemException(e.getMessage(), e);
		}
	}

	public String render(Page page, HttpServletRequest request, HttpServletResponse response, ParseConfig config)
			throws TplRenderException {
		return render(page, null, request, response, config);
	}

	private final String doRender(Page page, Map<String, Object> model, HttpServletRequest request,
			HttpServletResponse response, ParseConfig config) throws Exception {
		String templateName = TemplateUtils.getTemplateName(page);
		View view = thymeleafViewResolver.resolveViewName(templateName, request.getLocale());
		uiExposeHelper.addVariables(request);
		Stopwatch stopwatch = Stopwatch.createStarted();
		try {
			ParseContext.remove();
			ParseContext.setPage(page);
			ParseContext.setConfig(config);
			ResponseWrapper wrapper = new ResponseWrapper(response);
			view.render(model, request, wrapper);
			return wrapper.getRendered();
		} catch (Throwable e) {
			if (e instanceof RuntimeException || e instanceof Error) {
				markRollBack();
			}
			throw UIExceptionUtils.convert(templateName, e);
		} finally {
			commit();
			ParseContext.remove();

			stopwatch.stop();
			long renderMills = stopwatch.elapsed(TimeUnit.MILLISECONDS);
			TIME_LOGGER.debug("处理页面" + templateName + "耗费了" + renderMills + "ms");
		}
	}

	private void markRollBack() {
		TransactionStatus status = ParseContext.getTransactionStatus();
		if (status != null) {
			status.setRollbackOnly();
		}
	}

	private void commit() {
		TransactionStatus status = ParseContext.getTransactionStatus();
		if (status != null) {
			transactionManager.commit(status);
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

}
