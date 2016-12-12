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
package me.qyh.blog.web.listener;

import java.util.EnumSet;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.SessionCookieConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.WebApplicationContext;

import me.qyh.blog.config.UrlConfig;
import me.qyh.blog.web.filter.CORSFilter;
import me.qyh.blog.web.filter.UrlFilter;

public class AppContextLoaderListener extends ContextLoaderListener {

	private static final Logger logger = LoggerFactory.getLogger(AppContextLoaderListener.class);

	@Override
	public void contextInitialized(ServletContextEvent event) {
		super.contextInitialized(event);
		WebApplicationContext ctx = super.getCurrentWebApplicationContext();
		UrlConfig helper = ctx.getBean(UrlConfig.class);
		String domain = helper.getRootDomain();
		ServletContext sc = event.getServletContext();
		Class<? extends Filter> urlFilter = UrlFilter.class;
		sc.addFilter(urlFilter.getName(), urlFilter).addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), true,
				"/*");
		if (helper.isEnableSpaceDomain()) {
			logger.debug("开启了多域名支持，添加UrlFilter以转发请求,添加CORSFilter以处理跨域");
			Class<? extends Filter> corsFilter = CORSFilter.class;
			sc.addFilter(corsFilter.getName(), corsFilter).addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST),
					true, "/*");
			SessionCookieConfig config = sc.getSessionCookieConfig();
			config.setDomain(domain);
			String contextPath = sc.getContextPath();
			if (contextPath.isEmpty()) {
				config.setPath("/");
			} else {
				config.setPath(contextPath);
			}
		}
	}
}
