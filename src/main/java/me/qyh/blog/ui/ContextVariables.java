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
package me.qyh.blog.ui;

import java.util.Map;

import org.springframework.web.servlet.View;

import com.google.common.collect.ImmutableMap;

/**
 * request参数
 * 
 * @author Administrator
 *
 */
public final class ContextVariables {
	/**
	 * request attributes
	 */
	private final Map<String, Object> attributes;
	/**
	 * request params;
	 */
	private final Map<String, String[]> params;
	/**
	 * @see View#PATH_VARIABLES
	 */
	private final Map<String, Object> pathVariables;

	public ContextVariables(Map<String, Object> attributes, Map<String, String[]> params,
			Map<String, Object> pathVariables) {
		super();
		this.attributes = attributes == null ? ImmutableMap.of() : ImmutableMap.copyOf(attributes);
		this.params = params == null ? ImmutableMap.of() : ImmutableMap.copyOf(params);
		this.pathVariables = pathVariables == null ? ImmutableMap.of() : ImmutableMap.copyOf(pathVariables);
	}

	public ContextVariables() {
		this.attributes = ImmutableMap.of();
		this.params = ImmutableMap.of();
		this.pathVariables = ImmutableMap.of();
	}

	public String getParam(String param) {
		String[] values = params.get(param);
		if (values == null || values.length == 0) {
			return null;
		}
		return values[0];
	}

	public String[] getMutiParam(String param) {
		return params.get(param);
	}

	public Object getAttribute(String key) {
		return attributes.get(key);
	}

	public Object getPathVariable(String variable) {
		return pathVariables.get(variable);
	}

}