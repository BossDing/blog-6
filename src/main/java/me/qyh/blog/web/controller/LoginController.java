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
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import me.qyh.blog.bean.JsonResult;
import me.qyh.blog.config.Constants;
import me.qyh.blog.entity.User;
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.message.Message;
import me.qyh.blog.security.RememberMe;
import me.qyh.blog.security.csrf.CsrfToken;
import me.qyh.blog.security.csrf.CsrfTokenRepository;
import me.qyh.blog.service.UserService;
import me.qyh.blog.web.controller.form.LoginBean;
import me.qyh.blog.web.controller.form.LoginBeanValidator;

@Controller
public class LoginController extends BaseController {

	@Autowired
	private UserService userService;
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

	@RequestMapping(value = "login", method = RequestMethod.GET)
	public String login(Model ma) {
		ma.addAttribute(new LoginBean());
		return "login";
	}

	@RequestMapping(value = "/login", method = RequestMethod.POST, headers = "x-requested-with=XMLHttpRequest")
	@ResponseBody
	public JsonResult login(@RequestParam("validateCode") String validateCode,
			@RequestBody @Validated LoginBean loginBean, HttpServletRequest request, HttpServletResponse response) {
		HttpSession session = request.getSession(false);
		if (!Webs.matchValidateCode(validateCode, session))
			return new JsonResult(false, new Message("validateCode.error", "验证码错误"));
		User authenticated = null;
		try {
			authenticated = userService.login(loginBean);
		} catch (LogicException e) {
			rememberMe.remove(request, response);
			return new JsonResult(false, e.getLogicMessage());
		}
		if (loginBean.isRememberMe())
			rememberMe.save(authenticated, request, response);

		putInSession(authenticated, session);

		changeCsrf(request, response);
		return new JsonResult(true, new Message("login.success", "登录成功"));
	}

	@RequestMapping(value = "login", method = RequestMethod.POST)
	public String login(@RequestParam("validateCode") String validateCode, @Validated LoginBean loginBean,
			BindingResult result, Model model, HttpServletRequest request, HttpServletResponse response) {
		if (result.hasErrors()) {
			for (ObjectError error : result.getAllErrors()) {
				model.addAttribute(ERROR,
						new Message(error.getCode(), error.getDefaultMessage(), error.getArguments()));
				return "login";
			}
		}
		HttpSession session = request.getSession(false);
		if (!Webs.matchValidateCode(validateCode, session)) {
			model.addAttribute(ERROR, new Message("validateCode.error", "验证码错误"));
			return "login";
		}
		User authenticated = null;
		try {
			authenticated = userService.login(loginBean);
		} catch (LogicException e) {
			rememberMe.remove(request, response);
			model.addAttribute(ERROR, e.getLogicMessage());
			return "login";
		}
		if (loginBean.isRememberMe()) {
			rememberMe.save(authenticated, request, response);
		}

		putInSession(authenticated, session);

		changeCsrf(request, response);

		String lastAuthencationFailUrl = (String) session.getAttribute(Constants.LAST_AUTHENCATION_FAIL_URL);
		String redirectUrl = "/";
		if (lastAuthencationFailUrl != null) {
			redirectUrl = lastAuthencationFailUrl;
			session.removeAttribute(Constants.LAST_AUTHENCATION_FAIL_URL);
		}
		return "redirect:" + redirectUrl;
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

	private void putInSession(User authenticated, HttpSession session) {
		authenticated.setPassword(null);
		session.setAttribute(Constants.USER_SESSION_KEY, authenticated);
	}

}
