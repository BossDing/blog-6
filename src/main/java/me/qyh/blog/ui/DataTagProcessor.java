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

import javax.servlet.http.HttpServletRequest;

import org.springframework.context.ApplicationContext;
import org.thymeleaf.IEngineConfiguration;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.context.IWebContext;
import org.thymeleaf.exceptions.TemplateProcessingException;
import org.thymeleaf.model.IAttribute;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.processor.element.AbstractElementTagProcessor;
import org.thymeleaf.processor.element.IElementTagStructureHandler;
import org.thymeleaf.spring4.context.SpringContextUtils;
import org.thymeleaf.standard.expression.IStandardExpression;
import org.thymeleaf.standard.expression.IStandardExpressionParser;
import org.thymeleaf.standard.expression.StandardExpressions;
import org.thymeleaf.templatemode.TemplateMode;

import me.qyh.blog.service.UIService;
import me.qyh.blog.ui.data.DataBind;

/**
 * {@link http://www.thymeleaf.org/doc/tutorials/3.0/extendingthymeleaf.html#creating-our-own-dialect}
 * <p>
 * <h1>当data属性dynamic设置为true的时候，无法使用Params中的数据，并且会忽略查询过程中的一切异常</h1>
 * </p>
 * 
 * @see TemplateParser
 * @author mhlx
 *
 */
public class DataTagProcessor extends AbstractElementTagProcessor {

	private static final String TAG_NAME = "data";
	private static final int PRECEDENCE = 1000;
	private static final String NAME_ATTR = "name";
	private static final String DYNAMIC_ATT_PREFIX = "dt:";
	private static final String DYMAMIC_ATT = "dynamic";

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

			if (!check(context, tag)) {
				return;
			}

			DataTag dataTag = buildDataTag(context, tag);
			if (dataTag == null) {
				return;
			}

			DataBind<?> bind = null;
			try {
				bind = uiService.queryData(dataTag);
			} catch (Exception e) {
				// ignore;
			}

			if (bind != null) {
				IWebContext webContext = (IWebContext) context;
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

	private DataTag buildDataTag(ITemplateContext context, IProcessableElementTag tag) {
		DataTag dataTag = new DataTag(tag.getAttributeValue(NAME_ATTR));
		IAttribute[] atts = tag.getAllAttributes();
		final IEngineConfiguration configuration = context.getConfiguration();
		final IStandardExpressionParser parser = StandardExpressions.getExpressionParser(configuration);
		for (IAttribute att : atts) {
			String completeName = att.getAttributeCompleteName();
			if (completeName.startsWith(DYNAMIC_ATT_PREFIX)) {
				completeName = completeName.substring(3, completeName.length());

				if (dataTag.hasKey(completeName)) {
					continue;
				}

				// 动态属性
				String expression = att.getValue();
				IStandardExpression ex = parser.parseExpression(context, expression);
				Object obj = ex.execute(context);
				// 如果是dt:if标签并且解析值为true，返回null
				if (completeName.equalsIgnoreCase("if")) {
					if (parseBoolean(obj)) {
						return null;
					}
				} else if (obj != null) {
					dataTag.put(completeName, obj.toString());
				}
			} else {
				dataTag.put(completeName, att.getValue());
			}
		}
		return dataTag;
	}

	private boolean parseBoolean(Object obj) {
		if (obj == null) {
			return false;
		}
		if (obj instanceof Boolean) {
			return (Boolean) obj;
		}

		return false;
	}

	private boolean check(ITemplateContext context, IProcessableElementTag tag) {

		if (uiService == null) {
			ApplicationContext ctx = SpringContextUtils.getApplicationContext(context);
			uiService = ctx.getBean(UIService.class);
		}
		if (uiService == null) {
			// 不在spring环境中使用。。
			return false;
		}
		// 如果不是没有name属性和dynamic属性
		if (!tag.hasAttribute(NAME_ATTR) || !tag.hasAttribute(DYMAMIC_ATT)) {
			return false;
		}

		String dynamicAtt = tag.getAttributeValue(DYMAMIC_ATT);
		return Boolean.parseBoolean(dynamicAtt);
	}
}
