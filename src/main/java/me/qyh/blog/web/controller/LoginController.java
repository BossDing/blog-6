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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import me.qyh.blog.core.bean.JsonResult;
import me.qyh.blog.core.config.Constants;
import me.qyh.blog.core.config.UserConfig;
import me.qyh.blog.core.entity.User;
import me.qyh.blog.core.exception.LogicException;
import me.qyh.blog.core.message.Message;
import me.qyh.blog.core.security.BCrypts;
import me.qyh.blog.core.security.Environment;
import me.qyh.blog.core.security.RememberMe;
import me.qyh.blog.web.Webs;
import me.qyh.blog.web.controller.form.LoginBean;
import me.qyh.blog.web.controller.form.LoginBeanValidator;
import me.qyh.blog.web.security.CsrfToken;
import me.qyh.blog.web.security.CsrfTokenRepository;

@Controller("loginController")
public class LoginController extends AttemptLoggerController {

	@Autowired
	private RememberMe rememberMe;
	@Autowired
	private CsrfTokenRepository csrfTokenRepository;
	@Autowired
	private LoginBeanValidator loginBeanValidator;

	// 是否支持改变sessionid,需要运行容器支持servlet3.1+
	private static boolean SUPPORT_CHANGE_SESSION_ID;

	static {
		try {
			HttpServletRequest.class.getMethod("changeSessionId");
			SUPPORT_CHANGE_SESSION_ID = true;
		} catch (Exception e) {
			SUPPORT_CHANGE_SESSION_ID = false;
		}
	}

	@Value("${login.attempt.count:5}")
	private int attemptCount;

	@Value("${login.attempt.maxCount:100}")
	private int maxAttemptCount;

	@Value("${login.attempt.sleepSec:1800}")
	private int sleepSec;

	@InitBinder(value = "loginBean")
	protected void initBinder(WebDataBinder binder) {
		binder.setValidator(loginBeanValidator);
	}

	@PostMapping(value = "login")
	@ResponseBody
	public JsonResult login(@RequestBody @Validated LoginBean loginBean, HttpServletRequest request,
			HttpServletResponse response) {
		HttpSession session = request.getSession(false);
		String ip = Environment.getIP();
		if (log(ip) && !Webs.matchValidateCode(request.getParameter("validateCode"), session)) {
			return new JsonResult(false, new Message("validateCode.error", "验证码错误"));
		}
		try {
			doLogin(loginBean, request, response);

			remove(ip);

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

	@GetMapping("login/needCaptcha")
	@ResponseBody
	public boolean needCaptcha() {
		return reach(Environment.getIP());
	}

	private void doLogin(LoginBean loginBean, HttpServletRequest request, HttpServletResponse response)
			throws LogicException {
		User user = UserConfig.get();
		if (user.getName().equals(loginBean.getUsername())) {
			String encrptPwd = user.getPassword();
			if (BCrypts.matches(loginBean.getPassword(), encrptPwd)) {
				if (loginBean.isRememberMe()) {
					rememberMe.save(user, request, response);
				}

				request.getSession().setAttribute(Constants.USER_SESSION_KEY, user);
				if (SUPPORT_CHANGE_SESSION_ID) {
					request.changeSessionId();
				}
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

	@Override
	public void afterPropertiesSet() throws Exception {
		setAttemptLogger(new AttemptLogger(attemptCount, maxAttemptCount));
		setSleepSec(sleepSec);
		super.afterPropertiesSet();
	}

}
