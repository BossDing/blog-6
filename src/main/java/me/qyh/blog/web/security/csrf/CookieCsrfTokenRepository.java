package me.qyh.blog.web.security.csrf;

import java.util.UUID;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.util.WebUtils;

@Component
public final class CookieCsrfTokenRepository implements CsrfTokenRepository {

	static final String DEFAULT_CSRF_COOKIE_NAME = "XSRF-TOKEN";

	private String cookiePath;

	public CookieCsrfTokenRepository() {
		super();
	}

	@Override
	public CsrfToken generateToken(HttpServletRequest request) {
		return new CsrfToken(createNewToken());
	}

	@Override
	public void saveToken(CsrfToken token, HttpServletRequest request, HttpServletResponse response) {
		String tokenValue = token == null ? "" : token.getToken();
		Cookie cookie = new Cookie(DEFAULT_CSRF_COOKIE_NAME, tokenValue);
		cookie.setSecure(request.isSecure());
		if (this.cookiePath != null && !this.cookiePath.isEmpty()) {
			cookie.setPath(this.cookiePath);
		} else {
			cookie.setPath(this.getRequestContext(request));
		}
		if (token == null) {
			cookie.setMaxAge(0);
		} else {
			cookie.setMaxAge(-1);
		}
		cookie.setHttpOnly(true);
		response.addCookie(cookie);
	}

	@Override
	public CsrfToken loadToken(HttpServletRequest request) {
		Cookie cookie = WebUtils.getCookie(request, DEFAULT_CSRF_COOKIE_NAME);
		if (cookie == null) {
			return null;
		}
		String token = cookie.getValue();
		if (!StringUtils.hasLength(token)) {
			return null;
		}
		return new CsrfToken(token);
	}

	private String getRequestContext(HttpServletRequest request) {
		String contextPath = request.getContextPath();
		return contextPath.length() > 0 ? contextPath : "/";
	}

	private String createNewToken() {
		return UUID.randomUUID().toString();
	}

	/**
	 * Set the path that the Cookie will be created with. This will override the
	 * default functionality which uses the request context as the path.
	 *
	 * @param path
	 *            the path to use
	 */
	public void setCookiePath(String path) {
		this.cookiePath = path;
	}

	/**
	 * Get the path that the CSRF cookie will be set to.
	 *
	 * @return the path to be used.
	 */
	public String getCookiePath() {
		return this.cookiePath;
	}
}