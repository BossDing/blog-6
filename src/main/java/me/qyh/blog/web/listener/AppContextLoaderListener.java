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
		if (helper.isEnableSpaceDomain()) {
			logger.debug("开启了多域名支持，添加UrlFilter以转发请求,添加CORSFilter以处理跨域");
			Class<? extends Filter> urlFilter = UrlFilter.class;
			sc.addFilter(urlFilter.getName(), urlFilter).addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST),
					true, "/*");
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
