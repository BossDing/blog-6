package me.qyh.blog.web;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.springframework.web.HttpRequestHandler;

import me.qyh.blog.exception.SystemException;

public class MappingRegister {

	private Map<String, HttpRequestHandler> mapping = new HashMap<>();

	public final void registerMapping(String path, HttpRequestHandler handler) {
		Objects.requireNonNull(path);
		Objects.requireNonNull(handler);
		if (mapping.containsKey(path)) {
			throw new SystemException("路径:" + path + "已经存在");
		}
		mapping.put(path, handler);
	}

	public Map<String, Object> getMapping() {
		return Collections.unmodifiableMap(mapping);
	}

}
