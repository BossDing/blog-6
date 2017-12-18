package me.qyh.blog.file.store.local;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler;

import me.qyh.blog.core.exception.SystemException;

public class CustomResourceHttpRequestHandler extends ResourceHttpRequestHandler {
	private static final Logger logger = LoggerFactory.getLogger(CustomResourceHttpRequestHandler.class);

	@Override
	public void handleRequest(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			super.handleRequest(request, response);
		} catch (IOException e) {
			if (!response.isCommitted()) {
				Resource res = super.getResource(request);
				if (res == null || !res.exists()) {
					response.sendError(HttpServletResponse.SC_NOT_FOUND);
				} else {
					logger.debug(e.getMessage(), e);
					response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				}
			}

			return;
		}
	}

	protected String getPath(HttpServletRequest request) {
		String path = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
		if (path == null) {
			throw new IllegalStateException("Required request attribute '"
					+ HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE + "' is not set");
		}
		path = processPath(path);
		if (!StringUtils.hasText(path) || isInvalidPath(path)) {
			return null;
		}
		if (path.contains("%")) {
			try {
				// Use URLDecoder (vs UriUtils) to preserve potentially decoded UTF-8 chars
				if (isInvalidPath(URLDecoder.decode(path, "UTF-8"))) {
					return null;
				}
			} catch (IllegalArgumentException ex) {
				// ignore
			} catch (UnsupportedEncodingException ex) {
				throw new SystemException(ex.getMessage(), ex);
			}
		}
		return path;
	}

}
