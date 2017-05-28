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
package me.qyh.blog.web.thymeleaf;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.convert.ConversionService;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.util.CollectionUtils;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.support.RequestContext;
import org.springframework.web.servlet.view.AbstractTemplateView;
import org.thymeleaf.IEngineConfiguration;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebExpressionContext;
import org.thymeleaf.exceptions.TemplateProcessingException;
import org.thymeleaf.spring4.expression.ThymeleafEvaluationContext;
import org.thymeleaf.spring4.naming.SpringContextVariableNames;
import org.thymeleaf.spring4.view.ThymeleafViewResolver;
import org.thymeleaf.standard.expression.FragmentExpression;
import org.thymeleaf.standard.expression.IStandardExpressionParser;
import org.thymeleaf.standard.expression.StandardExpressionExecutionContext;
import org.thymeleaf.standard.expression.StandardExpressions;

import me.qyh.blog.core.exception.SystemException;

/**
 * 用来将模板解析成字符串
 * 
 * @author Administrator
 *
 */
public final class TemplateRender {

	private static final Logger TIME_LOGGER = LoggerFactory.getLogger(TemplateRender.class);

	@Autowired
	protected ThymeleafViewResolver thymeleafViewResolver;
	@Autowired
	private TemplateExposeHelper uiExposeHelper;
	@Autowired
	private PlatformTransactionManager transactionManager;

	@Autowired
	private ServletContext servletContext;
	@Autowired
	private ApplicationContext applicationContext;
	@Autowired
	private TemplateEngine viewTemplateEngine;

	private boolean exposeEvaluationContext = false;

	public String render(String templateName, Map<String, Object> model, HttpServletRequest request,
			HttpServletResponse response, ParseConfig config) throws TplRenderException {
		try {
			return doRender(templateName, model == null ? new HashMap<>() : model, request, response, config);
		} catch (TplRenderException | RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new SystemException(e.getMessage(), e);
		}
	}

	public String doRender(String templateName, Map<String, Object> model, HttpServletRequest request,
			HttpServletResponse response, ParseConfig config) throws Exception {
		uiExposeHelper.addVariables(request);
		long start = System.currentTimeMillis();
		ParseContext.setConfig(config);
		try {
			return doRender(templateName, model, request, response);
		} catch (Throwable e) {
			markRollBack();
			throw TemplateExceptionUtils.convert(templateName, e);
		} finally {
			commit();
			ParseContext.remove();

			long renderMills = System.currentTimeMillis() - start;
			TIME_LOGGER.debug("处理页面{0}耗费了{1}ms", templateName, renderMills);
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

	// ### copied from ThymeleafView

	private String doRender(String viewTemplateName, final Map<String, ?> model, final HttpServletRequest request,
			final HttpServletResponse response) throws Exception {

		Locale locale = LocaleContextHolder.getLocale();

		final Map<String, Object> mergedModel = new HashMap<>(30);
		@SuppressWarnings("unchecked")

		// View.PATH_VARIABLES 只能获取被PathVariable annotation属性标记的属性
		// 这里需要获取optional PathVariable
		final Map<String, Object> pathVars = (Map<String, Object>) request
				.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);

		if (pathVars != null) {
			mergedModel.putAll(pathVars);
		}
		if (model != null) {
			mergedModel.putAll(model);
		}

		final RequestContext requestContext = new RequestContext(request, response, servletContext, mergedModel);

		// For compatibility with ThymeleafView
		addRequestContextAsVariable(mergedModel, SpringContextVariableNames.SPRING_REQUEST_CONTEXT, requestContext);
		// For compatibility with AbstractTemplateView
		addRequestContextAsVariable(mergedModel, AbstractTemplateView.SPRING_MACRO_REQUEST_CONTEXT_ATTRIBUTE,
				requestContext);

		if (exposeEvaluationContext) {
			final ConversionService conversionService = (ConversionService) request
					.getAttribute(ConversionService.class.getName());// 可能为null
			final ThymeleafEvaluationContext evaluationContext = new ThymeleafEvaluationContext(applicationContext,
					conversionService);
			mergedModel.put(ThymeleafEvaluationContext.THYMELEAF_EVALUATION_CONTEXT_CONTEXT_VARIABLE_NAME,
					evaluationContext);
		}

		final IEngineConfiguration configuration = viewTemplateEngine.getConfiguration();
		final WebExpressionContext context = new WebExpressionContext(configuration, request,
				new ReadOnlyResponse(response), servletContext, locale, mergedModel);

		final String templateName;
		final Set<String> markupSelectors;
		if (!viewTemplateName.contains("::")) {
			// No fragment specified at the template name

			templateName = viewTemplateName;
			markupSelectors = null;

		} else {
			// Template name contains a fragment name, so we should parse it as
			// such

			final IStandardExpressionParser parser = StandardExpressions.getExpressionParser(configuration);

			final FragmentExpression fragmentExpression;
			try {
				// By parsing it as a standard expression, we might profit from
				// the expression cache
				fragmentExpression = (FragmentExpression) parser.parseExpression(context,
						"~{" + viewTemplateName + "}");
			} catch (final TemplateProcessingException e) {
				throw new IllegalArgumentException("Invalid template name specification: '" + viewTemplateName + "'",
						e);
			}

			final FragmentExpression.ExecutedFragmentExpression fragment = FragmentExpression
					.createExecutedFragmentExpression(context, fragmentExpression,
							StandardExpressionExecutionContext.NORMAL);

			templateName = FragmentExpression.resolveTemplateName(fragment);
			markupSelectors = FragmentExpression.resolveFragments(fragment);
			final Map<String, Object> nameFragmentParameters = fragment.getFragmentParameters();

			if (nameFragmentParameters != null) {

				if (fragment.hasSyntheticParameters()) {
					// We cannot allow synthetic parameters because there is no
					// way to specify them at the template
					// engine execution!
					throw new IllegalArgumentException(
							"Parameters in a view specification must be named (non-synthetic): '" + viewTemplateName
									+ "'");
				}

				context.setVariables(nameFragmentParameters);

			}

		}

		final Set<String> processMarkupSelectors;
		if (!CollectionUtils.isEmpty(markupSelectors)) {
			processMarkupSelectors = markupSelectors;
		} else {
			processMarkupSelectors = null;
		}

		return viewTemplateEngine.process(templateName, processMarkupSelectors, context);
	}

	private void addRequestContextAsVariable(final Map<String, Object> model, final String variableName,
			final RequestContext requestContext) throws TemplateProcessingException {

		if (model.containsKey(variableName)) {
			throw new TemplateProcessingException("属性" + variableName + "已经存在与request中");
		}
		model.put(variableName, requestContext);
	}

	public void setExposeEvaluationContext(boolean exposeEvaluationContext) {
		this.exposeEvaluationContext = exposeEvaluationContext;
	}

	private final class ReadOnlyResponse extends HttpServletResponseWrapper {

		public ReadOnlyResponse(HttpServletResponse response) {
			super(response);
		}

		@Override
		public void setResponse(ServletResponse response) {
			unsupport();
		}

		@Override
		public void setCharacterEncoding(String charset) {
			unsupport();
		}

		@Override
		public ServletOutputStream getOutputStream() throws IOException {
			unsupport();
			return null;
		}

		@Override
		public PrintWriter getWriter() throws IOException {
			unsupport();
			return null;
		}

		@Override
		public void setContentLength(int len) {
			unsupport();
		}

		@Override
		public void setContentLengthLong(long len) {
			unsupport();
		}

		@Override
		public void setContentType(String type) {
			unsupport();
		}

		@Override
		public void setBufferSize(int size) {
			unsupport();
		}

		@Override
		public void flushBuffer() throws IOException {
			unsupport();
		}

		@Override
		public void reset() {
			unsupport();
		}

		@Override
		public void resetBuffer() {
			unsupport();
		}

		@Override
		public void setLocale(Locale loc) {
			unsupport();
		}

		@Override
		public void addCookie(Cookie cookie) {
			unsupport();
		}

		@Override
		public void sendError(int sc, String msg) throws IOException {
			unsupport();
		}

		@Override
		public void sendError(int sc) throws IOException {
			unsupport();
		}

		@Override
		public void sendRedirect(String location) throws IOException {
			unsupport();
		}

		@Override
		public void setDateHeader(String name, long date) {
			unsupport();
		}

		@Override
		public void addDateHeader(String name, long date) {
			unsupport();
		}

		@Override
		public void setHeader(String name, String value) {
			unsupport();
		}

		@Override
		public void addHeader(String name, String value) {
			unsupport();
		}

		@Override
		public void setIntHeader(String name, int value) {
			unsupport();
		}

		@Override
		public void addIntHeader(String name, int value) {
			unsupport();
		}

		@Override
		public void setStatus(int sc) {
			unsupport();
		}

		@Override
		public void setStatus(int sc, String sm) {
			unsupport();
		}

		private void unsupport() {
			throw new TemplateProcessingException("不支持这个方法");
		}

	}
}
