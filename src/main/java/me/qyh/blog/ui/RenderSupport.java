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

import me.qyh.blog.ui.ParseContext.ParseStatus;

public class RenderSupport {

	@Autowired
	protected ThymeleafViewResolver thymeleafViewResolver;
	@Autowired
	private UIExposeHelper uiExposeHelper;
	@Autowired
	private PlatformTransactionManager transactionManager;

	private static final Logger TIME_LOGGER = LoggerFactory.getLogger(RenderSupport.class);

	protected final String render(String templateName, Map<String, Object> model, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		View view = thymeleafViewResolver.resolveViewName(templateName, request.getLocale());
		uiExposeHelper.addVariables(request);
		Stopwatch stopwatch = Stopwatch.createStarted();
		try {
			ResponseWrapper wrapper = new ResponseWrapper(response);
			view.render(model, request, wrapper);
			ParseContext.setStatus(ParseStatus.COMPLETE);
			return wrapper.getRendered();
		} catch (Throwable e) {
			if (e instanceof RuntimeException || e instanceof Error) {
				markRollBack();
			}
			ParseContext.setStatus(ParseStatus.BREAK);
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

	private static final class ResponseWrapper extends HttpServletResponseWrapper {

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
