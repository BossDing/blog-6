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

import me.qyh.blog.ui.page.ErrorPage;
import me.qyh.blog.ui.page.ErrorPage.ErrorCode;
import me.qyh.blog.ui.page.Page;
import me.qyh.blog.web.interceptor.SpaceContext;

@Controller
@RequestMapping(value = { "error", "space/{alias}/error" })
public class ErrorController {

	@RequestMapping("200")
	public Page handler200() {
		return handlerError(200);
	}

	@RequestMapping("400")
	public Page handler400() {
		return handlerError(400);
	}

	@RequestMapping("403")
	public Page handler403() {
		return handlerError(403);
	}

	@RequestMapping("404")
	public Page handler404() {
		return handlerError(404);
	}

	@RequestMapping("405")
	public Page handler405() {
		return handlerError(405);
	}

	@RequestMapping("500")
	public Page handler500() {
		return handlerError(500);
	}

	@RequestMapping("ui")
	public String handlerUI() {
		return "error/ui";
	}

	private Page handlerError(int error) {
		return new ErrorPage(SpaceContext.get(), ErrorCode.valueOf("ERROR_" + error));
	}

}
