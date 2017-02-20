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
package me.qyh.blog.file.local;

import java.util.HashMap;
import java.util.Map;

import org.springframework.web.HttpRequestHandler;

import me.qyh.blog.exception.SystemException;

/**
 * 
 * @author Administrator
 *
 */
class LocalResourceHttpRequestHandlerHolder {

	private static Map<String, Object> urlMap = new HashMap<>();

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
