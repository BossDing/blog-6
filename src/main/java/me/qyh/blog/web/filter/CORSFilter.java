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
package me.qyh.blog.web.filter;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.HttpMethod;
import org.springframework.util.StringUtils;
import org.springframework.web.context.support.WebApplicationContextUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import me.qyh.blog.config.UrlHelper;
import me.qyh.blog.exception.SystemException;
import me.qyh.blog.web.Webs;

public class CORSFilter extends OncePerRequestFilter {

	private UrlHelper urlHelper;

	private static final String DEFAULT_MAX_AGE = "1800";
	private static final String ORIGIN = "Origin";
	private static final String ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";
	private static final String ACCESS_CONTROL_ALLOW_METHODS = "Access-Control-Allow-Methods";
	private static final String ACCESS_CONTROL_ALLOW_HEADERS = "Access-Control-Allow-Headers";
	private static final String ACCESS_CONTROL_MAX_AGE = "Access-Control-Max-Age";
	private static final String ACCESS_CONTROL_ALLOW_CREDENTIALS = "Access-Control-Allow-Credentials";
	private static final Pattern FORWARDED_HOST_PATTERN = Pattern.compile("host=\"?([^;,\"]+)\"?");
	private static final Pattern FORWARDED_PROTO_PATTERN = Pattern.compile("proto=\"?([^;,\"]+)\"?");

	@Override
	protected void initFilterBean() throws ServletException {
		urlHelper = WebApplicationContextUtils.getRequiredWebApplicationContext(getServletContext())
				.getBean(UrlHelper.class);
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		if (Webs.isAction(request) && isCorsRequest(request)) {
			String origin = request.getHeader(ORIGIN);
			UriComponents actualUrl = fromHttpRequest(request).build();
			UriComponents originUrl = fromOriginalHeader(origin);
			if ((actualUrl.getHost().equals(originUrl.getHost()) && getPort(actualUrl) == getPort(originUrl))
					|| responseHasCors(response)) {
				filterChain.doFilter(request, response);
				return;
			}
			String actualOrigin = null;
			String host = originUrl.getHost();
			String rootDomain = urlHelper.getUrlConfig().getRootDomain();
			if (host.equals(rootDomain) || (host.endsWith(rootDomain) && (StringUtils.countOccurrencesOf(host,
					".") == StringUtils.countOccurrencesOf(rootDomain, ".") + 1))) {
				actualOrigin = origin;
			}
			if (actualOrigin == null) {
				response.sendError(403);
				return;
			}
			setHeader(actualOrigin, response);
		}
		filterChain.doFilter(request, response);
	}

	protected void setHeader(String origin, HttpServletResponse response) {
		response.addHeader(ACCESS_CONTROL_ALLOW_ORIGIN, origin);
		response.addHeader(ACCESS_CONTROL_ALLOW_METHODS, "GET, POST");
		response.addHeader(ACCESS_CONTROL_ALLOW_HEADERS, "Content-Type,x-csrf-token");
		response.addHeader(ACCESS_CONTROL_MAX_AGE, DEFAULT_MAX_AGE);
		response.setHeader(ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
	}

	protected boolean isCorsRequest(HttpServletRequest request) {
		return (request.getHeader("Origin") != null);
	}

	protected boolean isPreFlightRequest(HttpServletRequest request) {
		return (isCorsRequest(request) && request.getMethod().equals(HttpMethod.OPTIONS.name())
				&& request.getHeader("Access-Control-Request-Method") != null);
	}

	private UriComponents fromOriginalHeader(String origin) {
		UriComponentsBuilder builder = UriComponentsBuilder.newInstance();
		if (StringUtils.hasText(origin)) {
			int schemaIdx = origin.indexOf("://");
			String schema = (schemaIdx != -1 ? origin.substring(0, schemaIdx) : "http");
			builder.scheme(schema);
			String hostString = (schemaIdx != -1 ? origin.substring(schemaIdx + 3) : origin);
			if (hostString.contains(":")) {
				String[] hostAndPort = StringUtils.split(hostString, ":");
				builder.host(hostAndPort[0]);
				builder.port(Integer.parseInt(hostAndPort[1]));
			} else {
				builder.host(hostString);
			}
		}
		return builder.build();
	}

	private int getPort(UriComponents component) {
		int port = component.getPort();
		if (port == -1) {
			if ("http".equals(component.getScheme())) {
				port = 80;
			} else if ("https".equals(component.getScheme())) {
				port = 443;
			}
		}
		return port;
	}

	private boolean responseHasCors(HttpServletResponse resp) {
		boolean hasAllowOrigin = false;
		try {
			hasAllowOrigin = (resp.getHeader(ACCESS_CONTROL_ALLOW_ORIGIN) != null);
		} catch (NullPointerException npe) {
		}
		return hasAllowOrigin;
	}

	private UriComponentsBuilder fromHttpRequest(HttpServletRequest request) {
		URI uri = null;
		try {
			uri = new URI(request.getScheme(), null, request.getServerName(), request.getServerPort(),
					request.getRequestURI(), request.getQueryString(), null);
		} catch (URISyntaxException e) {
			throw new SystemException(e.getMessage(), e);
		}
		UriComponentsBuilder builder = UriComponentsBuilder.fromUri(uri);
		String scheme = uri.getScheme();
		String host = uri.getHost();
		int port = uri.getPort();

		String forwardedHeader = request.getHeader("Forwarded");
		if (StringUtils.hasText(forwardedHeader)) {
			String forwardedToUse = StringUtils.commaDelimitedListToStringArray(forwardedHeader)[0];
			Matcher m = FORWARDED_HOST_PATTERN.matcher(forwardedToUse);
			if (m.find()) {
				host = m.group(1).trim();
			}
			m = FORWARDED_PROTO_PATTERN.matcher(forwardedToUse);
			if (m.find()) {
				scheme = m.group(1).trim();
			}
		} else {
			String hostHeader = request.getHeader("X-Forwarded-Host");
			if (StringUtils.hasText(hostHeader)) {
				String[] hosts = StringUtils.commaDelimitedListToStringArray(hostHeader);
				String hostToUse = hosts[0];
				if (hostToUse.contains(":")) {
					String[] hostAndPort = StringUtils.split(hostToUse, ":");
					host = hostAndPort[0];
					port = Integer.parseInt(hostAndPort[1]);
				} else {
					host = hostToUse;
					port = -1;
				}
			}

			String portHeader = request.getHeader("X-Forwarded-Port");
			if (StringUtils.hasText(portHeader)) {
				String[] ports = StringUtils.commaDelimitedListToStringArray(portHeader);
				port = Integer.parseInt(ports[0]);
			}

			String protocolHeader = request.getHeader("X-Forwarded-Proto");
			if (StringUtils.hasText(protocolHeader)) {
				String[] protocols = StringUtils.commaDelimitedListToStringArray(protocolHeader);
				scheme = protocols[0];
			}
		}

		builder.scheme(scheme);
		builder.host(host);
		if (scheme.equals("http") && port != 80 || scheme.equals("https") && port != 443) {
			builder.port(port);
		}
		return builder;
	}

}