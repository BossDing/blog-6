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

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Maps;

import me.qyh.blog.exception.SystemException;
import me.qyh.blog.security.Environment;
import me.qyh.blog.service.SpaceService;
import me.qyh.blog.ui.ParseContext.ParseConfig;
import me.qyh.blog.ui.page.DisposiblePage;

/**
 * 用于渲染一次性页面等
 * 
 * @author Administrator
 *
 */
public class UIRender extends RenderSupport {

	@Autowired
	private SpaceService spaceService;

	public String render(DisposiblePage page, Map<String, Object> model, HttpServletRequest request,
			HttpServletResponse response, ParseConfig config) throws TplRenderException {
		// set space
		if (!Environment.hasSpace() && page.getSpace() != null) {
			Environment.setSpace(spaceService.getSpace(page.getSpace().getId())
					.orElseThrow(() -> new SystemException("空间:" + page.getSpace() + "不存在")));
		}
		try {
			return super.render(page, model == null ? Maps.newHashMap() : model, request, response, config);
		} catch (TplRenderException e) {
			throw e;
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new SystemException(e.getMessage(), e);
		}

	}

	public String render(DisposiblePage page, HttpServletRequest request, HttpServletResponse response,
			ParseConfig config) throws TplRenderException {
		return render(page, null, request, response, config);
	}

}
