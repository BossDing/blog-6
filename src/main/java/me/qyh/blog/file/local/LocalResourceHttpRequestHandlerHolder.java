package me.qyh.blog.file.local;

import java.util.HashMap;
import java.util.Map;

import org.springframework.web.HttpRequestHandler;

import me.qyh.blog.exception.SystemException;

class LocalResourceUrlMappingHolder {

	private static Map<String, Object> urlMap = new HashMap<String, Object>();

	static void put(String pattern, Object handler) {
		if (!(handler instanceof HttpRequestHandler)) {
			throw new SystemException("路径:" + pattern + "的handler必须为HttpRequestHandlerd的实现类");
		}
		if (urlMap.containsKey(pattern)) {
			throw new SystemException("路径:" + pattern + "已经存在");
		}
		urlMap.put(pattern, handler);
	}

	static Map<String, Object> get() {
		return urlMap;
	}

}
