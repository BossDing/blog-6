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
package me.qyh.blog.ui.page;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

import me.qyh.blog.service.UIService;
import me.qyh.blog.ui.Params;
import me.qyh.blog.ui.RenderedPage;
import me.qyh.blog.ui.UIContext;

public class ExpandedPageRequestController implements Controller {

	@Autowired
	private UIService uiService;
	@Autowired
	private ExpandedPageServer expandedPageServer;

	@Override
	public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
		ExpandedPageHandler handler = expandedPageServer.getPageHandler(request);
		Params params = handler.fromHttpRequest(request);
		if (params == null) {
			params = new Params();
		}
		RenderedPage page = uiService.renderExpandedPage(handler.id(), params);
		UIContext.set(page);
		return new ModelAndView(page.getTemplateName());
	}

}
