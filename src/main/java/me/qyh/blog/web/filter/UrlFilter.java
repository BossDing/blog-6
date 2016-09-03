package me.qyh.blog.web.filter;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import me.qyh.blog.config.UrlHelper;
import me.qyh.blog.web.controller.Webs;
import me.qyh.util.UrlUtils;

/**
 * 配置多域名访问时，用来将space.abc.com请求转发至abc.com/space
 * 
 * @author Administrator
 *
 */
public class UrlFilter extends OncePerRequestFilter {

	private UrlHelper urlHelper;

	private static final Logger logger = LoggerFactory.getLogger(UrlFilter.class);

	@Override
	protected void doFilterInternal(HttpServletRequest req, HttpServletResponse resp, FilterChain fc)
			throws ServletException, IOException {
		if (Webs.isAction(req)) {
			String space = urlHelper.getSpaceIfSpaceDomainRequest(req);
			if (space != null) {
				logger.debug("从空间域名请求中获取空间名:" + space);
				String requestUrl = buildForwardUrl(req, space);
				logger.debug(UrlUtils.buildFullRequestUrl(req) + "转发请求到:" + requestUrl);
				req.getRequestDispatcher(requestUrl).forward(req, resp);
				return;
			}
		}
		fc.doFilter(req, resp);
	}

	@Override
	protected void initFilterBean() throws ServletException {
		urlHelper = WebApplicationContextUtils.getRequiredWebApplicationContext(getServletContext())
				.getBean(UrlHelper.class);
	}

	private String buildForwardUrl(HttpServletRequest request, String space) {
		return "/space/" + space + request.getRequestURI().substring(request.getContextPath().length());
	}

}
