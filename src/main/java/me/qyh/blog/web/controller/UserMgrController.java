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

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import me.qyh.blog.core.bean.JsonResult;
import me.qyh.blog.core.config.Constants;
import me.qyh.blog.core.config.UserConfig;
import me.qyh.blog.core.entity.User;
import me.qyh.blog.core.exception.LogicException;
import me.qyh.blog.core.message.Message;
import me.qyh.blog.core.security.BCrypts;
import me.qyh.blog.web.controller.form.UserValidator;

@Controller
@RequestMapping("mgr/user")
public class UserMgrController extends BaseMgrController {

	@Autowired
	private UserValidator userValidator;

	@InitBinder(value = "user")
	protected void initBinder(WebDataBinder binder) {
		binder.setValidator(userValidator);
	}

	@GetMapping("index")
	public String index() {
		return "mgr/user/index";
	}

	@PostMapping("update")
	@ResponseBody
	public JsonResult update(@RequestParam(value = "oldPassword", defaultValue = "") String oldPassword,
			@Validated @RequestBody User user, HttpSession session) throws LogicException {
		if (!BCrypts.matches(oldPassword, UserConfig.get().getPassword())) {
			return new JsonResult(false, new Message("user.update.authFail", "密码验证失败"));
		}
		UserConfig.update(user);
		session.setAttribute(Constants.USER_SESSION_KEY, user);
		return new JsonResult(true, new Message("user.update.success", "更新成功"));
	}

}
