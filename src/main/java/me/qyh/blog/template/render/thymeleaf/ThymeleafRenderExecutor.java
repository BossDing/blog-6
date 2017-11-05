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
package me.qyh.blog.template.render.thymeleaf;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.convert.ConversionService;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.support.RequestContext;
import org.springframework.web.servlet.view.AbstractTemplateView;
import org.thymeleaf.IEngineConfiguration;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.IContext;
import org.thymeleaf.context.WebExpressionContext;
import org.thymeleaf.exceptions.TemplateProcessingException;
import org.thymeleaf.spring5.context.webmvc.SpringWebMvcThymeleafRequestContext;
import org.thymeleaf.spring5.expression.ThymeleafEvaluationContext;
import org.thymeleaf.spring5.naming.SpringContextVariableNames;

import me.qyh.blog.core.exception.SystemException;
import me.qyh.blog.template.render.ReadOnlyResponse;
import me.qyh.blog.template.render.TemplateRenderExecutor;

/**
 * 用来将模板解析成字符串
 * 
 * @author Administrator
 *
 */
public final class ThymeleafRenderExecutor implements TemplateRenderExecutor {

	@Autowired
	private ServletContext servletContext;
	@Autowired
	private ApplicationContext applicationContext;
	@Autowired
	private TemplateEngine viewTemplateEngine;

	private boolean exposeEvaluationContext = false;

	// COPIED FROM ThymeleafView 3.0.8.RELEASE
	@Override
	public String execute(String viewTemplateName, final Map<String, Object> model, final HttpServletRequest request,
			final ReadOnlyResponse response) {
		IContext context = buildIContext(viewTemplateName, model, request, response);
		return viewTemplateEngine.process(viewTemplateName, null, context);
	}

	private IContext buildIContext(String viewTemplateName, final Map<String, Object> model,
			final HttpServletRequest request, final HttpServletResponse response) {
		Objects.requireNonNull(viewTemplateName);
		if (viewTemplateName.contains("::")) {
			throw new SystemException("模板命中不能包含::");
		}

		Locale locale = LocaleContextHolder.getLocale();

		final Map<String, Object> mergedModel = new HashMap<>(30);

		// View.PATH_VARIABLES 只能获取被PathVariable annotation属性标记的属性
		// 这里需要获取optional PathVariable
		@SuppressWarnings("unchecked")
		final Map<String, Object> pathVars = (Map<String, Object>) request
				.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);

		if (pathVars != null) {
			mergedModel.putAll(pathVars);
		}
		if (model != null) {
			mergedModel.putAll(model);
		}

		final RequestContext requestContext = new RequestContext(request, response, servletContext, mergedModel);
		final SpringWebMvcThymeleafRequestContext thymeleafRequestContext = new SpringWebMvcThymeleafRequestContext(
				requestContext, request);

		// For compatibility with ThymeleafView
		addRequestContextAsVariable(mergedModel, SpringContextVariableNames.SPRING_REQUEST_CONTEXT, requestContext);
		// For compatibility with AbstractTemplateView
		addRequestContextAsVariable(mergedModel, AbstractTemplateView.SPRING_MACRO_REQUEST_CONTEXT_ATTRIBUTE,
				requestContext);
		// Add the Thymeleaf RequestContext wrapper that we will be using in
		// this dialect (the bare RequestContext
		// stays in the context to for compatibility with other dialects)
		mergedModel.put(SpringContextVariableNames.THYMELEAF_REQUEST_CONTEXT, thymeleafRequestContext);

		if (exposeEvaluationContext) {
			final ConversionService conversionService = (ConversionService) request
					.getAttribute(ConversionService.class.getName()); // might
																		// be
																		// null!
			final ThymeleafEvaluationContext evaluationContext = new ThymeleafEvaluationContext(applicationContext,
					conversionService);
			mergedModel.put(ThymeleafEvaluationContext.THYMELEAF_EVALUATION_CONTEXT_CONTEXT_VARIABLE_NAME,
					evaluationContext);
		}

		final IEngineConfiguration configuration = viewTemplateEngine.getConfiguration();
		final WebExpressionContext context = new WebExpressionContext(configuration, request, response, servletContext,
				locale, mergedModel);

		return context;
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
}
