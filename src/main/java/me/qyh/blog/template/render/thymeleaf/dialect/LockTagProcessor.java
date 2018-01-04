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

import javax.servlet.http.HttpServletRequest;

import org.springframework.context.ApplicationContext;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.context.IWebContext;
import org.thymeleaf.exceptions.TemplateProcessingException;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.processor.element.AbstractElementTagProcessor;
import org.thymeleaf.processor.element.IElementTagStructureHandler;
import org.thymeleaf.templatemode.TemplateMode;

import me.qyh.blog.core.entity.Lock;
import me.qyh.blog.core.entity.LockResource;
import me.qyh.blog.core.exception.LockException;
import me.qyh.blog.core.service.LockManager;
import me.qyh.blog.core.util.UrlUtils;
import me.qyh.blog.core.util.Validators;
import me.qyh.blog.core.vo.LockBean;
import me.qyh.blog.template.render.TemplateLockResource;
import me.qyh.blog.web.Webs;

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
	private static final String TYPE = "type";

	public static final String VARIABLE_NAME = LockTagProcessor.class.getName();

	public LockTagProcessor(String dialectPrefix, ApplicationContext applicationContext) {
		super(TemplateMode.HTML, // This processor will apply only to HTML mode
				dialectPrefix, // Prefix to be applied to name for matching
				TAG_NAME, // Tag name: match specifically this tag
				false, // Apply dialect prefix to tag name
				null, // No attribute name: will match by tag name
				false, // No prefix to be applied to attribute name
				PRECEDENCE); // Precedence (inside dialect's own precedence)
		this.lockManager = applicationContext.getBean(LockManager.class);
	}

	private final LockManager lockManager;

	@Override
	protected final void doProcess(ITemplateContext context, IProcessableElementTag tag,
			IElementTagStructureHandler structureHandler) {
		String lockId = tag.getAttributeValue(ID);
		if (Validators.isEmptyOrNull(lockId, true)) {
			structureHandler.removeElement();
			return;
		}
		boolean removed = false;
		try {
			String resourceId = context.getTemplateData().getTemplate();
			String type = tag.getAttributeValue(TYPE);
			boolean block = "block".equalsIgnoreCase(type);
			LockResource lockResource;
			if (block) {
				int line = tag.getLine();
				int col = tag.getCol();
				String location = "[" + line + "," + col + "]";
				lockResource = new TemplateLockResource(resourceId + location, lockId);
			} else {
				lockResource = new TemplateLockResource(resourceId, lockId);
			}

			LockException ex = null;
			Lock lock = null;
			try {
				lockManager.openLock(lockResource);
			} catch (LockException e) {
				ex = e;
				lock = e.getLock();
			}

			if (ex == null) {
				if (block) {
					structureHandler.setLocalVariable(VARIABLE_NAME, new LockStructure());
					structureHandler.removeTags();
					removed = true;
				}
			} else {
				if (!block) {
					throw ex;
				}

				if (context.getVariable(VARIABLE_NAME) != null) {
					throw new TemplateProcessingException("lock标签中不能嵌套lock标签");
				}

				IWebContext webCtx = (IWebContext) context;
				HttpServletRequest request = webCtx.getRequest();

				String redirectUrl = UrlUtils.buildFullRequestUrl(request);
				String alias = Webs.getSpaceFromRequest(request);

				LockBean lockBean = new LockBean(lock, lockResource, redirectUrl, alias);

				structureHandler.setLocalVariable(VARIABLE_NAME, new LockStructure(true, lockBean));
				structureHandler.removeTags();
				removed = true;
			}

		} finally {
			if (!removed) {
				structureHandler.removeElement();
			}
		}
	}

	public final class LockStructure {
		private final boolean locked;
		private final LockBean lockBean;

		public LockStructure() {
			this(false, null);
		}

		public LockStructure(boolean locked, LockBean lockBean) {
			super();
			this.locked = locked;
			this.lockBean = lockBean;
		}

		public boolean isLocked() {
			return locked;
		}

		public LockBean getLockBean() {
			return lockBean;
		}

	}
}
