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

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.convert.ConversionService;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.web.servlet.View;
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

import me.qyh.blog.core.entity.Space;
import me.qyh.blog.core.exception.SystemException;
import me.qyh.blog.core.security.Environment;
import me.qyh.blog.core.service.SpaceService;
import me.qyh.blog.core.ui.page.Page;

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
	private SpaceService spaceService;

	@Autowired
	private ServletContext servletContext;
	@Autowired
	private ApplicationContext applicationContext;
	@Autowired
	private TemplateEngine viewTemplateEngine;

	public String render(String templateName, Map<String, Object> model, HttpServletRequest request,
			HttpServletResponse response, ParseConfig config) throws TplRenderException {
		try {
			return doRender(templateName, model == null ? new HashMap<>() : model, request, response, config);
		} catch (TplRenderException e) {
			throw e;
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new SystemException(e.getMessage(), e);
		}
	}

	public String renderPreview(Page preview, HttpServletRequest request, HttpServletResponse response)
			throws TplRenderException {
		Space space = preview.getSpace();
		if (space != null) {
			space = spaceService.getSpace(space.getId()).orElse(null);
		}
		try {
			Environment.setSpace(space);
			ParseContext.setPreview(preview);
			return render(TemplateUtils.getPreviewTemplateName(), null, request, response,
					new ParseConfig(true, false));
		} finally {
			Environment.setSpace(null);
			ParseContext.remove();
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

	// ### copied from ThymeleafView

	private static final String pathVariablesSelector = View.PATH_VARIABLES;

	private String doRender(String viewTemplateName, final Map<String, ?> model, final HttpServletRequest request,
			final HttpServletResponse response) throws Exception {

		Locale locale = LocaleContextHolder.getLocale();

		final Map<String, Object> mergedModel = new HashMap<String, Object>(30);
		if (pathVariablesSelector != null) {
			@SuppressWarnings("unchecked")
			final Map<String, Object> pathVars = (Map<String, Object>) request.getAttribute(pathVariablesSelector);
			if (pathVars != null) {
				mergedModel.putAll(pathVars);
			}
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

		// Expose Thymeleaf's own evaluation context as a model variable
		//
		// Note Spring's EvaluationContexts are NOT THREAD-SAFE (in exchange for
		// SpelExpressions being thread-safe).
		// That's why we need to create a new EvaluationContext for each request
		// / template execution, even if it is
		// quite expensive to create because of requiring the initialization of
		// several ConcurrentHashMaps.
		final ConversionService conversionService = (ConversionService) request
				.getAttribute(ConversionService.class.getName()); // might be
																	// null!
		final ThymeleafEvaluationContext evaluationContext = new ThymeleafEvaluationContext(applicationContext,
				conversionService);
		mergedModel.put(ThymeleafEvaluationContext.THYMELEAF_EVALUATION_CONTEXT_CONTEXT_VARIABLE_NAME,
				evaluationContext);

		final IEngineConfiguration configuration = viewTemplateEngine.getConfiguration();
		final WebExpressionContext context = new WebExpressionContext(configuration, request, response, servletContext,
				locale, mergedModel);

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
				throw new IllegalArgumentException("Invalid template name specification: '" + viewTemplateName + "'");
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
		if (markupSelectors != null && markupSelectors.size() > 0) {
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
}
