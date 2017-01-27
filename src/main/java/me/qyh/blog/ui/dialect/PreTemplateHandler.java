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
import me.qyh.blog.ui.fragment.Fragment;
import me.qyh.blog.ui.page.Page;

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

			Page page = TemplateUtils.clone(ParseContext.getPage());
			page.setTpl("");
			((IEngineContext) context).setVariable("this", page);
		}
		if (TemplateUtils.isFragmentTemplate(template)) {

			Fragment fragment = TemplateUtils.clone(ParseContext.getFragment(template)
					.orElseThrow(() -> new TemplateProcessingException("模板" + template + "不存在")));
			fragment.setTpl("");

			((IEngineContext) context).setVariable("this", fragment);
		}
		super.setContext(context);
	}

}