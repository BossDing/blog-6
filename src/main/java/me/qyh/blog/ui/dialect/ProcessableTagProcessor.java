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

import java.util.Map;

import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.processor.element.IElementTagStructureHandler;
import org.thymeleaf.templatemode.TemplateMode;

import com.google.common.collect.Maps;

import me.qyh.blog.ui.UIStackoverflowError;

/**
 * 主要用来处理一些文本的渲染，通过设置text属性，来渲染text属性中的内容
 * <p>
 * <b> 如果用它来渲染request中的内容是非常危险的 </b>
 * </p>
 * 
 * @author Administrator
 *
 */
public class ProcessableTagProcessor extends DefaultAttributesTagProcessor {

	private static final String TAG_NAME = "processable";
	private static final int PRECEDENCE = 1000;

	public ProcessableTagProcessor(String dialectPrefix) {
		super(TemplateMode.HTML, // This processor will apply only to HTML mode
				dialectPrefix, // Prefix to be applied to name for matching
				TAG_NAME, // Tag name: match specifically this tag
				false, // Apply dialect prefix to tag name
				null, // No attribute name: will match by tag name
				false, // No prefix to be applied to attribute name
				PRECEDENCE); // Precedence (inside dialect's own precedence)
	}

	@Override
	protected void doProcess(ITemplateContext context, IProcessableElementTag tag,
			IElementTagStructureHandler structureHandler) {
		Map<String, String> attMap = Maps.newHashMap();
		processAttribute(context, tag, attMap);
		String text = attMap.get("utext");
		try {
			if (text == null || text.isEmpty()) {
				return;
			}
			structureHandler.replaceWith(context.getModelFactory().parse(context.getTemplateData(), text), true);
		} catch (StackOverflowError e) {
			String templateName = context.getTemplateData().getTemplate();
			if (tag.hasLocation()) {
				throw new UIStackoverflowError(templateName, tag.getCol(), tag.getLine(), e);
			} else {
				throw new UIStackoverflowError(templateName, null, null, e);
			}
		}
	}
}
