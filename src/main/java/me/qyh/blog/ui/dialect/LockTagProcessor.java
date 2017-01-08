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

import org.springframework.context.ApplicationContext;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.exceptions.TemplateProcessingException;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.processor.element.AbstractElementTagProcessor;
import org.thymeleaf.processor.element.IElementTagStructureHandler;
import org.thymeleaf.spring4.context.SpringContextUtils;
import org.thymeleaf.templatemode.TemplateMode;

import me.qyh.blog.lock.LockManager;
import me.qyh.blog.lock.SimpleLockResource;
import me.qyh.blog.security.Environment;

/**
 * {@link http://www.thymeleaf.org/doc/tutorials/3.0/extendingthymeleaf.html#creating-our-own-dialect}
 * 
 * @author mhlx
 *
 */
public class LockTagProcessor extends AbstractElementTagProcessor {

	private static final String TAG_NAME = "lock";
	private static final int PRECEDENCE = 1000;
	private static final String ID = "id";

	public LockTagProcessor(String dialectPrefix) {
		super(TemplateMode.HTML, // This processor will apply only to HTML mode
				dialectPrefix, // Prefix to be applied to name for matching
				TAG_NAME, // Tag name: match specifically this tag
				false, // Apply dialect prefix to tag name
				null, // No attribute name: will match by tag name
				false, // No prefix to be applied to attribute name
				PRECEDENCE); // Precedence (inside dialect's own precedence)
	}

	private LockManager lockManager;

	@Override
	protected final void doProcess(ITemplateContext context, IProcessableElementTag tag,
			IElementTagStructureHandler structureHandler) {
		checkLockManager(context);
		try {
			String lockId = tag.getAttributeValue(ID);
			if (lockId != null && !Environment.isLogin()) {
				String resourceId = context.getTemplateData().getTemplate();
				lockManager.openLock(new SimpleLockResource(resourceId, lockId));
			}
		} finally {
			structureHandler.removeElement();
		}
	}

	private void checkLockManager(ITemplateContext context) {
		if (lockManager == null) {
			ApplicationContext ctx = SpringContextUtils.getApplicationContext(context);
			if (ctx != null) {
				lockManager = ctx.getBean(LockManager.class);
			}
		}
		if (lockManager == null) {
			throw new TemplateProcessingException("没有可用的LockManager");
		}
	}
}
