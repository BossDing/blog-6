package me.qyh.blog.plugin.csrf.web.component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import me.qyh.blog.core.entity.User;
import me.qyh.blog.plugin.csrf.CsrfToken;
import me.qyh.blog.plugin.csrf.CsrfTokenRepository;
import me.qyh.blog.web.SuccessfulLoginHandler;

public class CsrfLoginHandler implements SuccessfulLoginHandler {

	private final CsrfTokenRepository csrfTokenRepository;

	public CsrfLoginHandler(CsrfTokenRepository csrfTokenRepository) {
		super();
		this.csrfTokenRepository = csrfTokenRepository;
	}

	@Override
	public void afterSuccessfulLogin(User user, HttpServletRequest request, HttpServletResponse response) {
		// 更改 csrf
		boolean containsToken = csrfTokenRepository.loadToken(request) != null;
		if (containsToken) {
			this.csrfTokenRepository.saveToken(null, request, response);

			CsrfToken newToken = this.csrfTokenRepository.generateToken(request);
			this.csrfTokenRepository.saveToken(newToken, request, response);
		}
	}

}
