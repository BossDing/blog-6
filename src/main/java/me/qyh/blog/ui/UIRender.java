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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Maps;

import me.qyh.blog.exception.SystemException;
import me.qyh.blog.service.SpaceService;
import me.qyh.blog.ui.page.DisposiblePage;
import me.qyh.blog.web.interceptor.SpaceContext;

/**
 * 用于渲染一次性页面等
 * 
 * @author Administrator
 *
 */
public class UIRender extends RenderSupport {

	@Autowired
	private SpaceService spaceService;

	public String render(DisposiblePage page, HttpServletRequest request, HttpServletResponse response)
			throws TplRenderException {
		// set space
		if (SpaceContext.get() == null && page.getSpace() != null) {
			SpaceContext.set(spaceService.getSpace(page.getSpace().getId()));
		}
		try {
			DisposablePageContext.set(page);
			return super.render(TemplateUtils.getTemplateName(page), Maps.newHashMap(), request, response);
		} catch (Exception e) {
			if (e instanceof TplRenderException) {
				throw (TplRenderException) e;
			}
			if (e instanceof RuntimeException) {
				throw (RuntimeException) e;
			}
			throw new SystemException(e.getMessage(), e);
		} finally {
			DisposablePageContext.clear();
		}

	}

}
