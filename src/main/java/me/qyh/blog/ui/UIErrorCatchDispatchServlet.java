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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.DispatcherServlet;

public class UIErrorCatchDispatchServlet extends DispatcherServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static final Logger logger = LoggerFactory.getLogger(UIErrorCatchDispatchServlet.class);

	@Override
	protected void doService(HttpServletRequest request, HttpServletResponse response) throws Exception {
		if (!response.isCommitted()) {
			try {
				super.doService(request, response);
			} catch (TplRenderException e) {
				Throwable ori = e.getOriginal();
				logger.error(ori.getMessage(), ori);
				if (!response.isCommitted()) {
					request.setAttribute("description", e.getRenderErrorDescription());
					request.getRequestDispatcher("/error/ui").forward(request, response);
				}
			}
		}
	}

}
