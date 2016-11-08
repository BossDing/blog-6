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
package me.qyh.blog.ui.page;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.util.CollectionUtils;

import me.qyh.blog.exception.SystemException;
import me.qyh.blog.web.controller.form.PageValidator;
import me.qyh.util.Validators;

public class ExpandedPageServer {

	private static final int NAME_MAX_LENGTH = 20;

	private Map<Integer, ExpandedPageHandler> handlers = new HashMap<Integer, ExpandedPageHandler>();

	public ExpandedPageHandler getPageHandler(HttpServletRequest request) {
		for (ExpandedPageHandler handler : handlers.values()) {
			if (handler.match(request)) {
				return handler;
			}
		}
		return null;
	}
	public boolean isEmpty() {
		return handlers.isEmpty();
	}

	public ExpandedPageHandler get(Integer id) {
		return handlers.get(id);
	}

	public boolean hasHandler(Integer id) {
		return handlers.containsKey(id);
	}

	public void setHandlers(List<ExpandedPageHandler> handlers) {
		if (!CollectionUtils.isEmpty(handlers)) {
			for (ExpandedPageHandler handler : handlers) {
				int id = handler.id();
				if (this.handlers.containsKey(id)) {
					throw new SystemException("拓展页面ID:" + id + "已经存在了");
				}
				String name = handler.name();
				if (Validators.isEmptyOrNull(name, true)) {
					throw new SystemException("拓展页面名称不能为空");
				}
				if (name.length() > NAME_MAX_LENGTH) {
					throw new SystemException("拓展页面名称不能超过" + NAME_MAX_LENGTH + "个字符");
				}
				String template = handler.getTemplate();
				if (template == null) {
					throw new SystemException("拓展页面模板不能为空");
				}
				if (template.length() > PageValidator.PAGE_TPL_MAX_LENGTH) {
					throw new SystemException("拓展页面模板不能超过" + PageValidator.PAGE_TPL_MAX_LENGTH + "个字符");
				}

				this.handlers.put(id, handler);
			}
		}
	}

	public Collection<ExpandedPageHandler> getHandlers() {
		return handlers.values();
	}

}
