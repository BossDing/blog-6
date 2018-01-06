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

import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.context.IWebContext;
import org.thymeleaf.exceptions.TemplateProcessingException;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.processor.element.AbstractElementTagProcessor;
import org.thymeleaf.processor.element.IElementTagStructureHandler;
import org.thymeleaf.templatemode.TemplateMode;

import me.qyh.blog.core.vo.LockBean;
import me.qyh.blog.template.render.thymeleaf.dialect.LockTagProcessor.LockStructure;
import me.qyh.blog.web.lock.LockHelper;

/**
 * 
 *
 */
public class LockedTagProcessor extends AbstractElementTagProcessor {

	private static final String TAG_NAME = "locked";
	private static final int PRECEDENCE = 1000;

	public LockedTagProcessor(String dialectPrefix) {
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
		LockStructure structure = (LockStructure) context.getVariable(LockTagProcessor.VARIABLE_NAME);
		if (structure == null) {
			throw new TemplateProcessingException("locked标签必须为lock标签的子标签");
		}
		if (structure.isLocked()) {
			LockBean bean = structure.getLockBean();
			LockHelper.storeLockBean(((IWebContext) context).getRequest(), bean);
			structureHandler.setLocalVariable("unlockId", bean.getId());
			structureHandler.removeTags();
		} else {
			structureHandler.removeElement();
		}
	}
}
