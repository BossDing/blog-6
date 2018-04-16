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
package me.qyh.blog.web.controller.front;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import me.qyh.blog.core.config.Constants;
import me.qyh.blog.core.context.Environment;
import me.qyh.blog.core.entity.User;
import me.qyh.blog.core.message.Message;
import me.qyh.blog.core.plugin.LogoutHandlerRegistry;
import me.qyh.blog.core.vo.JsonResult;
import me.qyh.blog.web.LogoutHandler;

@Controller
public class LogoutController implements LogoutHandlerRegistry {

	private static final Logger logger = LoggerFactory.getLogger(LogoutController.class);

	private List<LogoutHandler> handlers = new ArrayList<>();

	@PostMapping("logout")
	public String logout(HttpServletRequest request, HttpServletResponse response) {
		afterLogout(request, response);
		return "redirect:/";
	}

	@PostMapping(value = "logout", headers = "x-requested-with=XMLHttpRequest")
	@ResponseBody
	public JsonResult ajaxLogout(HttpServletRequest request, HttpServletResponse response) {
		afterLogout(request, response);
		return new JsonResult(true, new Message("logout.success", "注销成功"));
	}

	private void afterLogout(HttpServletRequest request, HttpServletResponse response) {
		HttpSession session = request.getSession(false);
		if (session != null) {
			User user = (User) session.getAttribute(Constants.USER_SESSION_KEY);
			if (user != null) {
				for (LogoutHandler handler : handlers) {
					try {
						handler.afterLogout(new User(user), request, response);
					} catch (Throwable e) {
						logger.warn(e.getMessage(), e);
					}
				}

				session.invalidate();
				Environment.setUser(null);
			}
		}
	}

	@Override
	public LogoutHandlerRegistry register(LogoutHandler handler) {
		handlers.add(handler);
		return this;
	}
}
