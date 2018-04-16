package me.qyh.blog.plugin.csrf.web.component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import me.qyh.blog.core.entity.User;
import me.qyh.blog.plugin.csrf.CsrfTokenRepository;
import me.qyh.blog.web.LogoutHandler;

public class CsrfLogoutHandler implements LogoutHandler {

	private final CsrfTokenRepository csrfTokenRepository;

	public CsrfLogoutHandler(CsrfTokenRepository csrfTokenRepository) {
		super();
		this.csrfTokenRepository = csrfTokenRepository;
	}

	@Override
	public void afterLogout(User user, HttpServletRequest request, HttpServletResponse response) {
		csrfTokenRepository.saveToken(null, request, response);
	}

}
