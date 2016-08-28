package me.qyh.blog.web.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import me.qyh.blog.security.RememberMe;
import me.qyh.blog.security.UserContext;
import me.qyh.blog.security.csrf.CsrfTokenRepository;

@Controller
public class LogoutController extends BaseController {

	@Autowired
	private RememberMe rememberMe;

	@Autowired
	private CsrfTokenRepository csrfTokenRepository;

	@RequestMapping(value = "logout", method = RequestMethod.POST)
	public String logout(HttpServletRequest request, HttpServletResponse response) {
		HttpSession session = request.getSession(false);
		if (session != null) {
			session.invalidate();
			UserContext.set(null);
			rememberMe.remove(request, response);
		}
		csrfTokenRepository.saveToken(null, request, response);
		return "redirect:/";
	}

}
