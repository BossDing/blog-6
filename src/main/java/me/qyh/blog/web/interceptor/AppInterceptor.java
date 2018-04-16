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
package me.qyh.blog.web.interceptor;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import me.qyh.blog.core.config.Constants;
import me.qyh.blog.core.config.UrlHelper;
import me.qyh.blog.core.context.Environment;
import me.qyh.blog.core.context.LockKeyContext;
import me.qyh.blog.core.entity.LockKey;
import me.qyh.blog.core.entity.Space;
import me.qyh.blog.core.entity.User;
import me.qyh.blog.core.exception.LockException;
import me.qyh.blog.core.exception.SpaceNotFoundException;
import me.qyh.blog.core.security.AuthencationException;
import me.qyh.blog.core.security.EnsureLogin;
import me.qyh.blog.core.service.LockManager;
import me.qyh.blog.core.service.SpaceService;
import me.qyh.blog.core.util.UrlUtils;
import me.qyh.blog.core.validator.SpaceValidator;
import me.qyh.blog.web.LockHelper;
import me.qyh.blog.web.Webs;

public class AppInterceptor extends HandlerInterceptorAdapter {

	private static final Logger LOGGER = LoggerFactory.getLogger(AppInterceptor.class);

	@Autowired
	private SpaceService spaceService;
	@Autowired
	private LockManager lockManager;
	@Autowired
	private UrlHelper urlHelper;

	private static final String UNLOCK_PATTERN = "/unlock/*";
	private static final String SPACE_UNLOCK_PATTERN = "/space/*/unlock/*";

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {
		if (isHandler(handler)) {

			try {
				setRequestAttribute(request);
				setUser(request, handler);
				setLockKeys(request);
				setSpace(request);
				Environment.setIP(Webs.getIP(request));
			} catch (AuthencationException | LockException | SpaceNotFoundException e) {
				removeContext();
				throw e;
			} catch (Throwable e) {
				removeContext();
				LOGGER.error(e.getMessage(), e);
				return false;
			}

		}
		return true;
	}

	private void setUser(HttpServletRequest request, Object handler) {
		HttpSession session = request.getSession(false);
		User user = null;
		if (session != null) {
			user = (User) session.getAttribute(Constants.USER_SESSION_KEY);
		}

		Environment.setUser(user);
		enableLogin(handler);
	}

	private void enableLogin(Object methodHandler) {
		if (methodHandler instanceof HandlerMethod) {
			// auth check
			getAnnotation(((HandlerMethod) methodHandler).getMethod(), EnsureLogin.class)
					.ifPresent(ann -> Environment.doAuthencation());
		}
	}

	private void setSpace(HttpServletRequest request) throws SpaceNotFoundException {
		String spaceAlias = Webs.getSpaceFromRequest(request);
		if (spaceAlias != null) {
			if (!SpaceValidator.isValidAlias(spaceAlias)) {
				throw new SpaceNotFoundException(spaceAlias);
			}
			Space space = spaceService.getSpace(spaceAlias).orElseThrow(() -> new SpaceNotFoundException(spaceAlias));

			if (!Webs.errorRequest(request)) {
				if (space.getIsPrivate()) {
					Environment.doAuthencation();
				}
				if (space.hasLock() && !Webs.unlockRequest(request)) {
					lockManager.openLock(space);
				}
			}

			Environment.setSpace(space);
		}
	}

	/**
	 * 将session中的解锁钥匙放入上下文中
	 * 
	 * @param request
	 */
	private void setLockKeys(HttpServletRequest request) {
		Map<String, List<LockKey>> keysMap = LockHelper.getKeysMap(request);
		if (!CollectionUtils.isEmpty(keysMap)) {
			LOGGER.debug("将LockKey放入LockKeyContext中:{}", keysMap);
			LockKeyContext.set(keysMap);
		}
	}

	private <T extends Annotation> Optional<T> getAnnotation(Method method, Class<T> annotationType) {
		T t = AnnotationUtils.findAnnotation(method, annotationType);
		if (t == null) {
			t = AnnotationUtils.findAnnotation(method.getDeclaringClass(), annotationType);
		}
		return Optional.ofNullable(t);
	}

	@Override
	public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
			throws Exception {
		removeContext();
	}

	@Override
	public void afterConcurrentHandlingStarted(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {
		removeContext();
	}

	private void removeContext() {
		Environment.remove();
		LockKeyContext.remove();
	}

	private void setRequestAttribute(HttpServletRequest request) {
		if (request.getAttribute(Webs.SPACE_ATTR_NAME) == null) {
			String path = request.getRequestURI().substring(request.getContextPath().length() + 1);
			request.setAttribute(Webs.SPACE_ATTR_NAME,
					Objects.toString(Webs.getSpaceFromPath(path, SpaceValidator.MAX_ALIAS_LENGTH + 1), ""));
		}
		String alias = Webs.getSpaceFromRequest(request);
		String unlockPattern = alias == null ? UNLOCK_PATTERN : SPACE_UNLOCK_PATTERN;
		if (request.getAttribute(Webs.UNLOCK_ATTR_NAME) == null) {
			String path = request.getRequestURI().substring(request.getContextPath().length());
			boolean isUnlock = UrlUtils.match(unlockPattern, path) && request.getParameter("unlockId") != null;
			request.setAttribute(Webs.UNLOCK_ATTR_NAME, isUnlock);
		}
		if (request.getAttribute(Webs.SPACE_URLS_ATTR_NAME) == null) {
			request.setAttribute(Webs.SPACE_URLS_ATTR_NAME, urlHelper.getUrlsBySpace(alias));
		}
	}

	private boolean isHandler(Object handler) {
		return handler instanceof HandlerMethod;
	}

}
