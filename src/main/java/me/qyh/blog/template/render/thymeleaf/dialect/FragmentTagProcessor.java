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

import java.util.Map;

import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.model.IModelFactory;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.processor.element.IElementTagStructureHandler;
import org.thymeleaf.templatemode.TemplateMode;

import me.qyh.blog.core.context.Environment;
import me.qyh.blog.template.render.thymeleaf.ThymeleafRenderExecutor;

/**
 * {@link http://www.thymeleaf.org/doc/tutorials/3.0/extendingthymeleaf.html#creating-our-own-dialect}
 * 
 * <p>
 * 同&lt;div th:replace="" &gt&lt;/div&gt;
 * </p>
 * 
 * @author mhlx
 *
 */
public class FragmentTagProcessor extends DefaultAttributesTagProcessor {

	private static final String TAG_NAME = "fragment";
	private static final int PRECEDENCE = 1000;
	private static final String NAME = "name";

	public FragmentTagProcessor(String dialectPrefix) {
		super(TemplateMode.HTML, dialectPrefix, TAG_NAME, false, null, false, PRECEDENCE);
	}

	@Override
	protected final void doProcess(ITemplateContext context, IProcessableElementTag tag,
			IElementTagStructureHandler structureHandler) {
		Map<String, String> attMap = processAttribute(context, tag);
		String name = attMap.get(NAME);
		if (name != null) {
			/**
			 * @since 5.9
			 */
			String templateName = ThymeleafRenderExecutor.getThymeleafFragmentName(name, Environment.getSpace());
			IModelFactory factory = context.getModelFactory();
			boolean replace = false;
			try {
				structureHandler.replaceWith(
						factory.parse(context.getTemplateData(), "<div th:replace=\"" + templateName + "\"></div>"),
						true);
				replace = true;
				return;
			} finally {
				if (!replace)
					structureHandler.removeElement();
			}
		}
		structureHandler.removeElement();
	}
}
