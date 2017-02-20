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

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import me.qyh.blog.exception.LogicException;
import me.qyh.blog.security.Environment;
import me.qyh.blog.ui.page.Page;
import me.qyh.blog.ui.page.UserPage;
import me.qyh.blog.web.controller.form.PageValidator;

@Controller
public class UserPageController {

	@RequestMapping(value = { "page/{alias}", "space/{alias}/page/{alias}" }, method = RequestMethod.GET)
	public Page index(@PathVariable("alias") String alias) throws LogicException {
		if (!PageValidator.validateUserPageAlias(alias)) {
			throw new LogicException("page.user.notExists", "页面不存在");
		}
		return new UserPage(Environment.getSpace().orElse(null), alias);
	}
}
