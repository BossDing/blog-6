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
package me.qyh.blog.plugin.csrf;

import static me.qyh.blog.web.WebExceptionResolver.ERROR_403;
import static me.qyh.blog.web.WebExceptionResolver.getErrorForward;
import static me.qyh.blog.web.WebExceptionResolver.getFullUrl;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.ModelAndView;

import me.qyh.blog.core.config.Constants;
import me.qyh.blog.core.vo.JsonResult;
import me.qyh.blog.web.ExceptionHandler;
import me.qyh.blog.web.WebExceptionResolver.ErrorInfo;
import me.qyh.blog.web.Webs;
import me.qyh.blog.web.view.JsonView;

public class CsrfExceptionHandler implements ExceptionHandler {

	@Override
	public boolean match(Exception ex) {
		return ex instanceof CsrfException;
	}

	@Override
	public ModelAndView handler(HttpServletRequest request, HttpServletResponse response, Exception ex) {
		if (Webs.isAjaxRequest(request)) {
			return new ModelAndView(new JsonView(new JsonResult(false, ERROR_403)));
		}
		// 将链接放入
		if ("get".equalsIgnoreCase(request.getMethod())) {
			request.getSession().setAttribute(Constants.LAST_AUTHENCATION_FAIL_URL, getFullUrl(request));
		}

		return getErrorForward(request, new ErrorInfo(ERROR_403, 403));
	}

}
