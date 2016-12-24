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

import java.io.Writer;
import java.util.Map;

import org.springframework.context.ApplicationContext;
import org.thymeleaf.TemplateSpec;
import org.thymeleaf.context.IEngineContext;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.context.IWebContext;
import org.thymeleaf.exceptions.TemplateProcessingException;
import org.thymeleaf.model.IAttribute;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.processor.element.AbstractElementTagProcessor;
import org.thymeleaf.processor.element.IElementTagStructureHandler;
import org.thymeleaf.spring4.context.SpringContextUtils;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.util.FastStringWriter;

import com.google.common.collect.Maps;

import me.qyh.blog.service.UIService;
import me.qyh.blog.ui.TemplateUtils;
import me.qyh.blog.ui.UIStackoverflowError;
import me.qyh.blog.ui.fragment.Fragment;

/**
 * {@link http://www.thymeleaf.org/doc/tutorials/3.0/extendingthymeleaf.html#creating-our-own-dialect}
 * 
 * @author mhlx
 *
 */
public class FragmentTagProcessor extends AbstractElementTagProcessor {

	private static final String TAG_NAME = "fragment";
	private static final String ATTRIBUTES = "attributes";
	private static final int PRECEDENCE = 1000;
	private static final String NAME = "name";

	public FragmentTagProcessor(String dialectPrefix) {
		super(TemplateMode.HTML, // This processor will apply only to HTML mode
				dialectPrefix, // Prefix to be applied to name for matching
				TAG_NAME, // Tag name: match specifically this tag
				false, // Apply dialect prefix to tag name
				null, // No attribute name: will match by tag name
				false, // No prefix to be applied to attribute name
				PRECEDENCE); // Precedence (inside dialect's own precedence)
	}

	private UIService uiService;

	@Override
	protected final void doProcess(ITemplateContext context, IProcessableElementTag tag,
			IElementTagStructureHandler structureHandler) {
		if (uiService == null) {
			ApplicationContext ctx = SpringContextUtils.getApplicationContext(context);
			if (ctx != null) {
				uiService = ctx.getBean(UIService.class);
			}
		}
		if (uiService == null) {
			structureHandler.removeElement();
			return;
		}
		IAttribute nameAtt = tag.getAttribute(NAME);
		if (nameAtt != null) {
			String name = nameAtt.getValue();
			Fragment fragment = uiService.queryFragment(name);
			if (fragment != null) {
				Attributes attributes = handleAttributes(tag);
				if (attributes == null) {
					attributes = new Attributes();
				}
				IWebContext iEngineContext = (IWebContext) context;
				((IEngineContext) context).setVariable(ATTRIBUTES, attributes);
				String templateName = TemplateUtils.getTemplateName(fragment);
				Writer writer = new FastStringWriter(200);
				try {
					context.getConfiguration().getTemplateManager().parseAndProcess(
							new TemplateSpec(templateName, null, TemplateMode.HTML, null), iEngineContext, writer);
					structureHandler.replaceWith(writer.toString(), false);
				} catch (Exception e) {
					structureHandler.removeElement();
					throw new TemplateProcessingException(e.getMessage(), e);
				} catch (StackOverflowError e) {
					structureHandler.removeElement();
					if (tag.hasLocation()) {
						throw new UIStackoverflowError(templateName, tag.getCol(), tag.getLine(), e);
					} else {
						throw new UIStackoverflowError(templateName, null, null, e);
					}
				} finally {
					((IEngineContext) context).removeVariable(ATTRIBUTES);
				}
				return;
			}
		}
		structureHandler.removeElement();
	}

	protected Attributes handleAttributes(IProcessableElementTag tag) {
		Attributes attributes = new Attributes();
		IAttribute[] attributArray = tag.getAllAttributes();
		for (IAttribute attribute : attributArray) {
			String v = attribute.getValue();
			if (v != null) {
				attributes.put(attribute.getAttributeCompleteName(), v);
			}
		}
		return attributes;
	}

	protected class Attributes {
		private Map<String, String> map = Maps.newHashMap();

		public String get(String key) {
			return map.get(key);
		}

		public double getDouble(String key, double defaultV) {
			String v = get(key);
			if (v != null) {
				return Double.parseDouble(v);
			}
			return defaultV;
		}

		public long getLong(String key, long defaultV) {
			String v = get(key);
			if (v != null) {
				return Long.parseLong(v);
			}
			return defaultV;
		}

		public int getInt(String key, int defaultV) {
			String v = get(key);
			if (v != null) {
				return Integer.parseInt(v);
			}
			return defaultV;
		}

		public boolean getBoolean(String key, boolean defaultV) {
			String v = get(key);
			if (v != null) {
				return Boolean.parseBoolean(v);
			}
			return defaultV;
		}

		public Attributes() {
		}

		private void put(String key, String v) {
			map.put(key, v);
		}
	}
}
