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
package me.qyh.blog.web.controller;

import javax.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.HandlerMapping;

import me.qyh.blog.exception.LogicException;
import me.qyh.blog.security.Environment;
import me.qyh.blog.ui.TemplateUtils;
import me.qyh.blog.ui.page.Page;
import me.qyh.blog.ui.page.UserPage;
import me.qyh.blog.web.controller.form.PageValidator;

@Controller
public class UserPageController {

	@RequestMapping(value = { "page/**", "space/{alias}/page/**" }, method = RequestMethod.GET)
	public Page index(HttpServletRequest request) throws LogicException {
		String alias = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
		if (alias != null) {
			alias = TemplateUtils.cleanUserPageAlias(alias);
			if (alias.startsWith("page")) {
				alias = alias.substring(5);
			} else {
				// space length + 1 + page length + 1 + 1 = 12
				alias = alias.substring(12 + Environment.getSpaceAlias().map(String::length).orElse(0));
			}
			if (PageValidator.validateUserPageAlias(alias, false)) {
				return new UserPage(Environment.getSpace().orElse(null), alias);
			}
		}
		throw new LogicException("page.user.notExists", "页面不存在");
	}
}
