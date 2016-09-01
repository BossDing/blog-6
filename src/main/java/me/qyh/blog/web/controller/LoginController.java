package me.qyh.blog.web.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import me.qyh.blog.config.Constants;
import me.qyh.blog.entity.User;
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.message.Message;
import me.qyh.blog.security.RememberMe;
import me.qyh.blog.security.csrf.CsrfToken;
import me.qyh.blog.security.csrf.CsrfTokenRepository;
import me.qyh.blog.service.UserService;
import me.qyh.blog.web.controller.form.LoginBean;

@Controller
public class LoginController extends BaseController {

	@Autowired
	private UserService userService;
	@Autowired
	private RememberMe rememberMe;
	@Autowired
	private CsrfTokenRepository csrfTokenRepository;

	@RequestMapping(value = "login", method = RequestMethod.GET)
	public String login(Model ma) {
		ma.addAttribute(new LoginBean());
		return "login";
	}

	@RequestMapping(value = "login", method = RequestMethod.POST)
	public String login(LoginBean loginBean, Model model, HttpServletRequest request, HttpServletResponse response) {
		HttpSession session = request.getSession(false);
		if (!Webs.matchValidateCode(loginBean.getValidateCode(), session)) {
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
		authenticated.setPassword(null);
		session.setAttribute(Constants.USER_SESSION_KEY, authenticated);

		// 更改 csrf
		boolean containsToken = csrfTokenRepository.loadToken(request) != null;
		if (containsToken) {
			this.csrfTokenRepository.saveToken(null, request, response);

			CsrfToken newToken = this.csrfTokenRepository.generateToken(request);
			this.csrfTokenRepository.saveToken(newToken, request, response);
		}

		String lastAuthencationFailUrl = (String) session.getAttribute(Constants.LAST_AUTHENCATION_FAIL_URL);
		String redirectUrl = "/";
		if (lastAuthencationFailUrl != null) {
			redirectUrl = lastAuthencationFailUrl;
			session.removeAttribute(Constants.LAST_AUTHENCATION_FAIL_URL);
		}
		return "redirect:" + redirectUrl;
	}

}
