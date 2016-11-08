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
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.thymeleaf.spring4.view.ThymeleafView;

public class _ThymeleafView extends ThymeleafView {

	@Override
	protected void renderFragment(Set<String> markupSelectorsToRender, Map<String, ?> model, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		try {
			super.renderFragment(markupSelectorsToRender, model, request, response);
		} catch (Throwable e) {
			throw new TplRenderException(TplRenderExceptionHandler.getHandler().convert(e, getServletContext()), e);
		}
	}

}
