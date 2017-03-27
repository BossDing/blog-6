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
package me.qyh.blog.web.controller;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.util.CollectionUtils;
import org.springframework.web.servlet.HandlerMapping;

import me.qyh.blog.web.TemplateView;

public class TemplateController {

	private final String templateName;

	public TemplateController(String templateName) {
		super();
		this.templateName = templateName;
	}

	public TemplateView handleRequest(HttpServletRequest request) {
		@SuppressWarnings("unchecked")
		Map<String, Object> pathVariables = (Map<String, Object>) request
				.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
		if (!CollectionUtils.isEmpty(pathVariables)) {
			for (Map.Entry<String, Object> it : pathVariables.entrySet()) {
				if (request.getAttribute(it.getKey()) != null) {
					continue;
				}
				Object v = it.getValue();
				if (v == null) {
					continue;
				}
				request.setAttribute(it.getKey(), v);
			}
		}
		return new TemplateView(templateName);
	}

	public String getTemplateName() {
		return templateName;
	}
}
