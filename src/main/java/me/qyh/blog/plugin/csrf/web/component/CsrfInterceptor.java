package me.qyh.blog.plugin.csrf.web.component;

import java.util.Objects;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import me.qyh.blog.core.util.Validators;
import me.qyh.blog.plugin.csrf.CsrfException;
import me.qyh.blog.plugin.csrf.CsrfToken;
import me.qyh.blog.plugin.csrf.CsrfTokenRepository;
import me.qyh.blog.plugin.csrf.InvalidCsrfTokenException;
import me.qyh.blog.plugin.csrf.MissingCsrfTokenException;
import me.qyh.blog.web.Webs;
import me.qyh.blog.web.security.RequestMatcher;

@Component
public class CsrfInterceptor implements HandlerInterceptor {
	private static final Logger LOGGER = LoggerFactory.getLogger(CsrfInterceptor.class);

	@Autowired
	private CsrfTokenRepository tokenRepository;

	private RequestMatcher requireCsrfProtectionMatcher = new DefaultRequiresCsrfMatcher();

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {
		if (handler instanceof HandlerMethod) {
			try {
				csrfCheck(request, response);
			} catch (CsrfException e) {
				throw e;
			} catch (Throwable e) {
				LOGGER.error(e.getMessage(), e);
				return false;
			}
		}
		return true;
	}

	private void csrfCheck(HttpServletRequest request, HttpServletResponse response) {
		CsrfToken csrfToken = tokenRepository.loadToken(request);
		final boolean missingToken = csrfToken == null;
		if (missingToken) {
			CsrfToken generatedToken = tokenRepository.generateToken(request);
			csrfToken = new SaveOnAccessCsrfToken(tokenRepository, request, response, generatedToken);
		}
		request.setAttribute(CsrfToken.class.getName(), csrfToken);
		request.setAttribute(csrfToken.getParameterName(), csrfToken);
		if ("get".equalsIgnoreCase(request.getMethod())) {
			// GET请求不能检查，否则死循环
			return;
		}

		/**
		 * @since 5.9
		 */
		if (Webs.errorRequest(request)) {
			return;
		}

		if (!requireCsrfProtectionMatcher.match(request)) {
			return;
		}
		String actualToken = request.getHeader(csrfToken.getHeaderName());
		if (actualToken == null) {
			actualToken = request.getParameter(csrfToken.getParameterName());
		}
		if (!csrfToken.getToken().equals(actualToken)) {
			if (missingToken) {
				throw new MissingCsrfTokenException(actualToken);
			} else {
				throw new InvalidCsrfTokenException(csrfToken, actualToken);
			}
		}
	}

	@SuppressWarnings("serial")
	private static final class SaveOnAccessCsrfToken implements CsrfToken {
		private transient CsrfTokenRepository tokenRepository;
		private transient HttpServletRequest request;
		private transient HttpServletResponse response;

		private final CsrfToken delegate;

		SaveOnAccessCsrfToken(CsrfTokenRepository tokenRepository, HttpServletRequest request,
				HttpServletResponse response, CsrfToken delegate) {
			super();
			this.tokenRepository = tokenRepository;
			this.request = request;
			this.response = response;
			this.delegate = delegate;
		}

		@Override
		public String getHeaderName() {
			return delegate.getHeaderName();
		}

		@Override
		public String getParameterName() {
			return delegate.getParameterName();
		}

		@Override
		public String getToken() {
			saveTokenIfNecessary();
			return delegate.getToken();
		}

		@Override
		public String toString() {
			return "SaveOnAccessCsrfToken [delegate=" + delegate + "]";
		}

		@Override
		public int hashCode() {
			return Objects.hash(delegate);
		}

		@Override
		public boolean equals(Object obj) {
			if (Validators.baseEquals(this, obj)) {
				SaveOnAccessCsrfToken other = (SaveOnAccessCsrfToken) obj;
				return Objects.equals(this.delegate, other.delegate);
			}
			return false;
		}

		private void saveTokenIfNecessary() {
			if (this.tokenRepository == null) {
				return;
			}

			synchronized (this) {
				if (tokenRepository != null) {
					this.tokenRepository.saveToken(delegate, request, response);
					this.tokenRepository = null;
					this.request = null;
					this.response = null;
				}
			}
		}

	}

	private static final class DefaultRequiresCsrfMatcher implements RequestMatcher {
		private final Pattern allowedMethods = Pattern.compile("^(GET|HEAD|TRACE|OPTIONS)$");

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.springframework.security.web.util.matcher.RequestMatcher#matches(
		 * javax.servlet.http.HttpServletRequest)
		 */
		public boolean match(HttpServletRequest request) {
			return !allowedMethods.matcher(request.getMethod()).matches();
		}
	}

	public void setRequireCsrfProtectionMatcher(RequestMatcher requireCsrfProtectionMatcher) {
		this.requireCsrfProtectionMatcher = requireCsrfProtectionMatcher;
	}
}
