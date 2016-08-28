package me.qyh.blog.web.interceptor;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;
import org.thymeleaf.exceptions.TemplateProcessingException;

import me.qyh.blog.config.Constants;
import me.qyh.blog.config.UrlHelper;
import me.qyh.blog.entity.Space;
import me.qyh.blog.entity.User;
import me.qyh.blog.exception.SpaceNotFoundException;
import me.qyh.blog.file.local.RequestMatcher;
import me.qyh.blog.lock.LockHelper;
import me.qyh.blog.lock.LockKey;
import me.qyh.blog.lock.LockKeyContext;
import me.qyh.blog.message.Messages;
import me.qyh.blog.security.AuthencationException;
import me.qyh.blog.security.EnsureLogin;
import me.qyh.blog.security.RememberMe;
import me.qyh.blog.security.UserContext;
import me.qyh.blog.security.csrf.CsrfToken;
import me.qyh.blog.security.csrf.CsrfTokenRepository;
import me.qyh.blog.security.csrf.InvalidCsrfTokenException;
import me.qyh.blog.security.csrf.MissingCsrfTokenException;
import me.qyh.blog.service.SpaceService;
import me.qyh.blog.ui.Template;
import me.qyh.blog.ui.UIContext;
import me.qyh.blog.web.controller.GlobalControllerExceptionHandler;
import me.qyh.util.UrlUtils;

public class AppInterceptor extends HandlerInterceptorAdapter {

	private static final Logger logger = LoggerFactory.getLogger(AppInterceptor.class);

	@Autowired
	private UrlHelper urlHelper;
	@Autowired
	private RememberMe rememberMe;
	@Autowired
	private Messages messages;
	@Autowired
	private SpaceService spaceService;
	@Autowired
	private CsrfTokenRepository tokenRepository;

	private RequestMatcher requireCsrfProtectionMatcher = new DefaultRequiresCsrfMatcher();

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {
		if (handler instanceof HandlerMethod) {

			csrfCheck(request, response);

			HttpSession session = request.getSession(false);
			User user = null;
			if (session != null) {
				user = (User) session.getAttribute(Constants.USER_SESSION_KEY);
			}
			if (user == null) {
				// auto login
				try {
					user = autoLogin(request, response);
				} catch (Throwable e) {
					logger.error(e.getMessage(), e);
				}
			}

			enableLogin(handler, user);
			setLockKeys(request);
			UserContext.set(user);

			String spaceAlias = urlHelper.getUrls(request).getSpace();
			if (spaceAlias != null) {
				Space space = spaceService.selectSpaceByAlias(spaceAlias);
				if (space == null) {
					throw new SpaceNotFoundException(spaceAlias);
				}
				SpaceContext.set(space);
			}
		}
		return true;
	}

	private void enableLogin(Object methodHandler, User user) {
		// auth check
		EnsureLogin ensureLogin = getAnnotation(((HandlerMethod) methodHandler).getMethod(), EnsureLogin.class);
		if (ensureLogin != null && user == null) {
			throw new AuthencationException();
		}
	}

	/**
	 * 通过cookie自动登录
	 * 
	 * @param request
	 * @param response
	 * @return
	 */
	private User autoLogin(HttpServletRequest request, HttpServletResponse response) {
		// auto login
		User user = rememberMe.login(request, response);
		if (user != null) {
			logger.debug("用户没有登录，自动登录成功");
			user.setPassword(null);
			request.getSession().setAttribute(Constants.USER_SESSION_KEY, user);
		}
		return user;
	}

	/**
	 * 将session中的解锁钥匙放入上下文中
	 * 
	 * @param request
	 */
	private void setLockKeys(HttpServletRequest request) {
		Map<String, LockKey> keysMap = LockHelper.getKeysMap(request);
		if (!CollectionUtils.isEmpty(keysMap)) {
			logger.debug("将LockKey放入LockKeyContext中:" + keysMap);
			LockKeyContext.set(keysMap);
		}
	}

	private <T extends Annotation> T getAnnotation(Method method, Class<T> annotationType) {
		T t = AnnotationUtils.findAnnotation(method, annotationType);
		if (t == null) {
			t = AnnotationUtils.findAnnotation(method.getDeclaringClass(), annotationType);
		}
		return t;
	}

	@Override
	public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
			ModelAndView modelAndView) throws Exception {
		// ajax request
		// static res request
		// then mv is null
		if (modelAndView != null) {
			Template tpl = UIContext.get();
			if (tpl != null) {
				logger.debug("将模板数据放入model中");
				modelAndView.addAllObjects(tpl.getTemplateDatas());
			}
			logger.debug("将用户和路径处理器放入model中");
			modelAndView.addObject("urls", urlHelper.getUrls(request));
			modelAndView.addObject("user", UserContext.get());
			modelAndView.addObject("messages", messages);
			modelAndView.addObject("space", SpaceContext.get());
		}
	}

	/**
	 * 如果抛出异常，首先会被GlobalControllerExceptionHandler所捕获处理<br/>
	 * 但GlobalControllerExceptionHandler无法处理页面渲染时发生的异常
	 * 
	 * @see GlobalControllerExceptionHandler
	 */
	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
			throws Exception {
		if (handler instanceof HandlerMethod) {
			UIContext.remove();
			UserContext.remove();
			LockKeyContext.remove();
			SpaceContext.remove();
		}
		if (ex != null && (ex instanceof TemplateProcessingException)) {
			logger.error(ex.getMessage(), ex);
		}
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

		if (!requireCsrfProtectionMatcher.match(request)) {
			return;
		}

		String actualToken = request.getHeader(csrfToken.getHeaderName());
		if (actualToken == null) {
			actualToken = request.getParameter(csrfToken.getParameterName());
		}
		if (!csrfToken.getToken().equals(actualToken)) {
			if (logger.isDebugEnabled()) {
				logger.debug("Invalid CSRF token found for " + UrlUtils.buildFullRequestUrl(request));
			}
			if (missingToken) {
				throw new MissingCsrfTokenException(actualToken);
			} else {
				throw new InvalidCsrfTokenException(csrfToken, actualToken);
			}
		}
	}

	public void setRequireCsrfProtectionMatcher(RequestMatcher requireCsrfProtectionMatcher) {
		this.requireCsrfProtectionMatcher = requireCsrfProtectionMatcher;
	}

	@SuppressWarnings("serial")
	private static final class SaveOnAccessCsrfToken implements CsrfToken {
		private transient CsrfTokenRepository tokenRepository;
		private transient HttpServletRequest request;
		private transient HttpServletResponse response;

		private final CsrfToken delegate;

		public SaveOnAccessCsrfToken(CsrfTokenRepository tokenRepository, HttpServletRequest request,
				HttpServletResponse response, CsrfToken delegate) {
			super();
			this.tokenRepository = tokenRepository;
			this.request = request;
			this.response = response;
			this.delegate = delegate;
		}

		public String getHeaderName() {
			return delegate.getHeaderName();
		}

		public String getParameterName() {
			return delegate.getParameterName();
		}

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
			final int prime = 31;
			int result = 1;
			result = prime * result + ((delegate == null) ? 0 : delegate.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			SaveOnAccessCsrfToken other = (SaveOnAccessCsrfToken) obj;
			if (delegate == null) {
				if (other.delegate != null)
					return false;
			} else if (!delegate.equals(other.delegate))
				return false;
			return true;
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
		private Pattern allowedMethods = Pattern.compile("^(GET|HEAD|TRACE|OPTIONS)$");

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * org.springframework.security.web.util.matcher.RequestMatcher#matches(
		 * javax.servlet.http.HttpServletRequest)
		 */
		public boolean match(HttpServletRequest request) {
			return !allowedMethods.matcher(request.getMethod()).matches();
		}
	}

}
