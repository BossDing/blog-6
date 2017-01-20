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

import org.thymeleaf.context.IEngineContext;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.engine.AbstractTemplateHandler;
import org.thymeleaf.exceptions.TemplateProcessingException;

import me.qyh.blog.ui.ParseContext;
import me.qyh.blog.ui.TemplateUtils;
import me.qyh.blog.ui.TplResolver.FragmentResource;
import me.qyh.blog.ui.TplResolver.PageResource;

/**
 * 不希望通过replace等方式再次渲染页面
 * 
 * @author mhlx
 *
 */
public final class PreTemplateHandler extends AbstractTemplateHandler {
	public PreTemplateHandler() {
		super();
	}

	@Override
	public void setContext(ITemplateContext context) {
		String template = context.getTemplateData().getTemplate();
		if (TemplateUtils.isPageTemplate(template)) {
			if (ParseContext.isStart()) {
				throw new TemplateProcessingException("无法再次处理页面");
			}
			ParseContext.start();

			PageResource pageResource = (PageResource) context.getTemplateData().getTemplateResource();
			((IEngineContext) context).setVariable("this", pageResource.getPage());
		}
		if (TemplateUtils.isFragmentTemplate(template)) {
			FragmentResource fragmentResource = (FragmentResource) context.getTemplateData().getTemplateResource();
			((IEngineContext) context).setVariable("this", fragmentResource.getFragment());
		}
		super.setContext(context);
	}

}