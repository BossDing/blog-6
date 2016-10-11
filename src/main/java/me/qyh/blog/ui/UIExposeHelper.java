package me.qyh.blog.ui;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;

import me.qyh.blog.config.UrlHelper;
import me.qyh.blog.message.Messages;
import me.qyh.blog.security.UserContext;
import me.qyh.blog.web.interceptor.SpaceContext;

public class UIExposeHelper {

	@Autowired
	private UrlHelper urlHelper;
	@Autowired
	private Messages messages;

	public Map<String, Object> getHelpers(HttpServletRequest request) {
		Map<String, Object> helpers = new HashMap<>();
		helpers.put("urls", urlHelper.getUrls(request));
		helpers.put("user", UserContext.get());
		helpers.put("messages", messages);
		helpers.put("space", SpaceContext.get());
		return helpers;
	}
}
