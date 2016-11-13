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

import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import org.thymeleaf.context.IEngineContext;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.engine.TemplateModel;
import org.thymeleaf.model.IAttribute;
import org.thymeleaf.model.IModel;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.processor.element.AbstractElementTagProcessor;
import org.thymeleaf.processor.element.IElementTagStructureHandler;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.util.FastStringWriter;

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
	private static final String TEMPLATE_NAME = "templateName";

	public FragmentTagProcessor(String dialectPrefix) {
		super(TemplateMode.HTML, // This processor will apply only to HTML mode
				dialectPrefix, // Prefix to be applied to name for matching
				TAG_NAME, // Tag name: match specifically this tag
				false, // Apply dialect prefix to tag name
				null, // No attribute name: will match by tag name
				false, // No prefix to be applied to attribute name
				PRECEDENCE); // Precedence (inside dialect's own precedence)
	}

	@Override
	protected final void doProcess(ITemplateContext context, IProcessableElementTag tag,
			IElementTagStructureHandler structureHandler) {
		RenderedPage page = UIContext.get();
		if (page != null) {
			IAttribute nameAtt = tag.getAttribute(NAME);
			if (nameAtt != null) {
				String name = nameAtt.getValue();
				Fragment fragment = page.getFragmentMap().get(name);
				if (fragment != null) {
					Attributes attributes = handleAttributes(tag);
					if (attributes == null) {
						attributes = new Attributes();
					}
					IEngineContext iEngineContext = (IEngineContext) context;
					iEngineContext.setVariable(ATTRIBUTES, attributes);
					iEngineContext.setVariable(TEMPLATE_NAME, page.getTemplateName());
					IModel model = context.getModelFactory().parse(context.getTemplateData(), fragment.getTpl());
					try (Writer writer = new FastStringWriter(200)) {
						context.getConfiguration().getTemplateManager().process((TemplateModel) model, iEngineContext,
								writer);
						structureHandler.replaceWith(writer.toString(), false);
					} catch (Exception e) {
						throw new FragmentTagParseException(e, name);
					} finally {
						iEngineContext.removeVariable(ATTRIBUTES);
					}
					return;
				}
			}
		}
		structureHandler.removeElement();
	}

	public static final class FragmentTagParseException extends RuntimeException {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private final Throwable originalThrowable;
		private final String fragment;

		public FragmentTagParseException(Throwable originalThrowable, String fragment) {
			this.originalThrowable = originalThrowable;
			this.fragment = fragment;
		}

		public Throwable getOriginalThrowable() {
			return originalThrowable;
		}

		public String getFragment() {
			return fragment;
		}

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
		private Map<String, String> map = new HashMap<>();

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
