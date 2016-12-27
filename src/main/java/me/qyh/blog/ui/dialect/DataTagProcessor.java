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
package me.qyh.blog.ui.dialect;

import java.util.Enumeration;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.context.ApplicationContext;
import org.springframework.web.servlet.View;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.context.IWebContext;
import org.thymeleaf.exceptions.TemplateProcessingException;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.processor.element.IElementTagStructureHandler;
import org.thymeleaf.spring4.context.SpringContextUtils;
import org.thymeleaf.templatemode.TemplateMode;

import com.google.common.collect.Maps;

import me.qyh.blog.exception.LogicException;
import me.qyh.blog.exception.RuntimeLogicException;
import me.qyh.blog.service.UIService;
import me.qyh.blog.ui.ContextVariables;
import me.qyh.blog.ui.DataTag;
import me.qyh.blog.ui.DisposablePageContext;
import me.qyh.blog.ui.data.DataBind;
import me.qyh.blog.util.Validators;

/**
 * {@link http://www.thymeleaf.org/doc/tutorials/3.0/extendingthymeleaf.html#creating-our-own-dialect}
 * 
 * @author mhlx
 *
 */
public class DataTagProcessor extends DefaultAttributesTagProcessor {

	private static final String TAG_NAME = "data";
	private static final int PRECEDENCE = 1000;
	private static final String NAME_ATTR = "name";

	private UIService uiService;

	public DataTagProcessor(String dialectPrefix) {
		super(TemplateMode.HTML, dialectPrefix, // Prefix to be applied to name
												// for matching
				TAG_NAME, // Tag name: match specifically this tag
				false, // Apply dialect prefix to tag name
				null, // No attribute name: will match by tag name
				false, // No prefix to be applied to attribute name
				PRECEDENCE); // Precedence (inside dialect's own precedence)
	}

	@Override
	protected final void doProcess(ITemplateContext context, IProcessableElementTag tag,
			IElementTagStructureHandler structureHandler) {
		try {
			check(context, tag);

			DataTag dataTag = buildDataTag(context, tag);

			if (dataTag == null) {
				return;
			}

			DataBind<?> bind = null;
			IWebContext webContext = (IWebContext) context;
			if (DisposablePageContext.get() != null && DisposablePageContext.get().isPreview()) {
				bind = uiService.queryPreviewData(dataTag);
			} else {
				try {
					bind = uiService.queryData(dataTag, buildContextVariables(webContext));
				} catch (LogicException e) {
					throw new RuntimeLogicException(e);
				}
			}
			if (bind != null) {
				HttpServletRequest request = webContext.getRequest();
				if (request.getAttribute(bind.getDataName()) != null) {
					throw new TemplateProcessingException("属性" + bind.getDataName() + "已经存在于request中");
				}
				webContext.getRequest().setAttribute(bind.getDataName(), bind.getData());
			}
		} finally {
			structureHandler.removeElement();
		}
	}

	private ContextVariables buildContextVariables(IWebContext webContext) {
		HttpServletRequest request = webContext.getRequest();
		@SuppressWarnings("unchecked")
		Map<String, Object> pathVariables = (Map<String, Object>) request.getAttribute(View.PATH_VARIABLES);
		Map<String, String[]> paramsMap = request.getParameterMap();
		Map<String, Object> attributes = Maps.newHashMap();
		Enumeration<String> enAttr = request.getAttributeNames();
		while (enAttr.hasMoreElements()) {
			String attributeName = enAttr.nextElement();
			attributes.put(attributeName, request.getAttribute(attributeName));
		}
		return new ContextVariables(attributes, paramsMap, pathVariables);
	}

	private DataTag buildDataTag(ITemplateContext context, IProcessableElementTag tag) {
		Map<String, String> attMap = Maps.newHashMap();

		processAttribute(context, tag, attMap);

		String name = attMap.get(NAME_ATTR);
		if (Validators.isEmptyOrNull(name, true)) {
			return null;
		}
		return new DataTag(name, attMap);
	}

	private void check(ITemplateContext context, IProcessableElementTag tag) {

		if (uiService == null) {
			ApplicationContext ctx = SpringContextUtils.getApplicationContext(context);
			if (ctx != null) {
				uiService = ctx.getBean(UIService.class);
			}
		}
		if (uiService == null) {
			throw new TemplateProcessingException("没有可用的UIService");
		}
	}

}
