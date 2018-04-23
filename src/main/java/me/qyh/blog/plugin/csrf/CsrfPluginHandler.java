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
package me.qyh.blog.plugin.csrf;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.context.ApplicationContext;
import org.springframework.web.servlet.HandlerInterceptor;

import me.qyh.blog.core.plugin.ExceptionHandlerRegistry;
import me.qyh.blog.core.plugin.HandlerInterceptorRegistry;
import me.qyh.blog.core.plugin.LogoutHandlerRegistry;
import me.qyh.blog.core.plugin.PluginHandler;
import me.qyh.blog.core.plugin.PluginProperties;
import me.qyh.blog.core.plugin.SuccessfulLoginHandlerRegistry;
import me.qyh.blog.plugin.csrf.web.component.CsrfInterceptor;
import me.qyh.blog.plugin.csrf.web.component.CsrfLoginHandler;
import me.qyh.blog.plugin.csrf.web.component.CsrfLogoutHandler;

public class CsrfPluginHandler implements PluginHandler {

	private CsrfInterceptor csrfInterceptor;
	private CsrfTokenRepository repository;

	private static final String ENABLE_KEY = "plugin.csrf.enable";

	private CsrfToken emptyToken;

	private boolean enable = PluginProperties.getInstance().get(ENABLE_KEY).map(Boolean::parseBoolean).orElse(false);

	@Override
	public void init(ApplicationContext applicationContext) throws Exception {
		csrfInterceptor = applicationContext.getBean(CsrfInterceptor.class);
		repository = applicationContext.getBean(CsrfTokenRepository.class);
	}

	@Override
	public void addHandlerInterceptor(HandlerInterceptorRegistry registry) throws Exception {
		if (enable) {
			registry.register(csrfInterceptor);
		} else {
			registry.register(new HandlerInterceptor() {

				@Override
				public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
						throws Exception {
					if (emptyToken == null) {
						emptyToken = repository.generateToken(request);
					}
					request.setAttribute(CsrfToken.class.getName(), emptyToken);
					request.setAttribute(emptyToken.getParameterName(), emptyToken);
					return true;
				}
			});
		}
	}

	@Override
	public void addSuccessfulLoginHandler(SuccessfulLoginHandlerRegistry registry) throws Exception {
		if (enable) {
			registry.registry(new CsrfLoginHandler(repository));
		}
	}

	@Override
	public void addLogoutHandler(LogoutHandlerRegistry registry) throws Exception {
		if (enable) {
			registry.register(new CsrfLogoutHandler(repository));
		}
	}

	@Override
	public void addExceptionHandler(ExceptionHandlerRegistry registry) throws Exception {
		if (enable) {
			registry.register(new CsrfExceptionHandler());
		}
	}

}
