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
import org.springframework.web.bind.annotation.RequestMapping;

import me.qyh.blog.core.exception.LogicException;
import me.qyh.blog.core.security.Environment;
import me.qyh.blog.core.ui.page.Page;
import me.qyh.blog.core.ui.page.SysPage;
import me.qyh.blog.core.ui.page.SysPage.PageTarget;

@Controller
public class ErrorController {

	@RequestMapping(value = { "error", "space/{alias}/error" })
	public Page error() throws LogicException {
		return new SysPage(Environment.getSpace(), PageTarget.ERROR);
	}

	@RequestMapping(value = { "error/ui" })
	public String handlerUI() {
		return "error/ui";
	}
}
