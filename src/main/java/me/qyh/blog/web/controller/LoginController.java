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
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import me.qyh.blog.core.bean.JsonResult;
import me.qyh.blog.core.config.Constants;
import me.qyh.blog.core.config.UserConfig;
import me.qyh.blog.core.entity.User;
import me.qyh.blog.core.exception.LogicException;
import me.qyh.blog.core.message.Message;
import me.qyh.blog.core.security.BCrypts;
import me.qyh.blog.core.security.RememberMe;
import me.qyh.blog.web.Webs;
import me.qyh.blog.web.controller.form.LoginBean;
import me.qyh.blog.web.controller.form.LoginBeanValidator;
import me.qyh.blog.web.security.CsrfToken;
import me.qyh.blog.web.security.CsrfTokenRepository;

@Controller
public class LoginController extends BaseController {

	@Autowired
	private RememberMe rememberMe;
	@Autowired
	private CsrfTokenRepository csrfTokenRepository;
	@Autowired
	private LoginBeanValidator loginBeanValidator;

	@InitBinder(value = "loginBean")
	protected void initBinder(WebDataBinder binder) {
		binder.setValidator(loginBeanValidator);
	}

	@RequestMapping(value = "/login", method = RequestMethod.POST, headers = "x-requested-with=XMLHttpRequest")
	@ResponseBody
	public JsonResult login(@RequestParam("validateCode") String validateCode,
			@RequestBody @Validated LoginBean loginBean, HttpServletRequest request, HttpServletResponse response) {
		HttpSession session = request.getSession(false);
		if (!Webs.matchValidateCode(validateCode, session)) {
			return new JsonResult(false, new Message("validateCode.error", "验证码错误"));
		}
		try {
			login(loginBean, request, response);

			String lastAuthencationFailUrl = (String) session.getAttribute(Constants.LAST_AUTHENCATION_FAIL_URL);
			if (lastAuthencationFailUrl != null) {
				session.removeAttribute(Constants.LAST_AUTHENCATION_FAIL_URL);
			}
			return new JsonResult(true, lastAuthencationFailUrl);
		} catch (LogicException e) {
			rememberMe.remove(request, response);
			return new JsonResult(false, new Message("user.loginFail", "登录失败"));
		}
	}

	private void login(LoginBean loginBean, HttpServletRequest request, HttpServletResponse response)
			throws LogicException {
		User user = UserConfig.get();
		if (user.getName().equals(loginBean.getUsername())) {
			String encrptPwd = user.getPassword();
			if (BCrypts.matches(loginBean.getPassword(), encrptPwd)) {
				if (loginBean.isRememberMe()) {
					rememberMe.save(user, request, response);
				}

				request.getSession().setAttribute(Constants.USER_SESSION_KEY, user);

				changeCsrf(request, response);
				return;
			}
		}
		throw new LogicException("user.loginFail", "登录失败");
	}

	private void changeCsrf(HttpServletRequest request, HttpServletResponse response) {
		// 更改 csrf
		boolean containsToken = csrfTokenRepository.loadToken(request) != null;
		if (containsToken) {
			this.csrfTokenRepository.saveToken(null, request, response);

			CsrfToken newToken = this.csrfTokenRepository.generateToken(request);
			this.csrfTokenRepository.saveToken(newToken, request, response);
		}
	}

}
