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
package me.qyh.blog.template.render.thymeleaf.dialect;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.thymeleaf.TemplateSpec;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.exceptions.TemplateProcessingException;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.processor.element.IElementTagStructureHandler;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.util.FastStringWriter;

import me.qyh.blog.core.context.Environment;
import me.qyh.blog.core.text.Markdown2Html;
import me.qyh.blog.template.entity.Fragment;
import me.qyh.blog.template.render.thymeleaf.UIStackoverflowError;

/**
 * {@link http://www.thymeleaf.org/doc/tutorials/3.0/extendingthymeleaf.html#creating-our-own-dialect}
 * 
 * @author mhlx
 *
 */
public class FragmentTagProcessor extends DefaultAttributesTagProcessor {

	private static final Logger logger = LoggerFactory.getLogger(FragmentTagProcessor.class);

	private static final String TAG_NAME = "fragment";
	private static final int PRECEDENCE = 1000;
	private static final String NAME = "name";
	private static final String TYPE = "type";

	private final Markdown2Html markdown2Html;

	public FragmentTagProcessor(String dialectPrefix, ApplicationContext ctx) {
		super(TemplateMode.HTML, dialectPrefix, TAG_NAME, false, null, false, PRECEDENCE);
		this.markdown2Html = ctx.getBean(Markdown2Html.class);
	}

	@Override
	protected final void doProcess(ITemplateContext context, IProcessableElementTag tag,
			IElementTagStructureHandler structureHandler) {
		Map<String, String> attMap = processAttribute(context, tag);
		String name = attMap.get(NAME);
		if (name != null) {
			String templateName = Fragment.getTemplateName(name, Environment.getSpace());
			try (Writer writer = new FastStringWriter(200)) {

				context.getConfiguration().getTemplateManager().parseAndProcess(
						new TemplateSpec(templateName, null, TemplateMode.HTML, null), context, writer);

				String content = writer.toString();

				/**
				 * @since 5.9
				 */
				FragmentType type = getFragmentType(attMap.get(TYPE));
				switch (type) {
				case HTML:
					structureHandler.replaceWith(content, false);
					break;
				case MARKDOWN:
					if (content.trim().isEmpty()) {
						structureHandler.replaceWith(content, false);
					} else {
						structureHandler.replaceWith(markdown2Html.toHtml(content), false);
					}
					break;
				default:
					throw new TemplateProcessingException("无法处理的FragmentType:" + type);
				}

				return;
			} catch (StackOverflowError e) {
				if (tag.hasLocation()) {
					throw new UIStackoverflowError(templateName, tag.getCol(), tag.getLine(), e);
				} else {
					throw new UIStackoverflowError(templateName, null, null, e);
				}
			} catch (IOException e) {
				// ??
				logger.debug(e.getMessage(), e);
			}
		}
		structureHandler.removeElement();
	}

	protected final FragmentType getFragmentType(String type) {
		if (type == null || type.isEmpty()) {
			return FragmentType.HTML;
		}
		try {
			return FragmentType.valueOf(type.toUpperCase());
		} catch (Exception e) {
			return FragmentType.HTML;
		}
	}

	protected enum FragmentType {
		HTML, MARKDOWN;
	}

}
