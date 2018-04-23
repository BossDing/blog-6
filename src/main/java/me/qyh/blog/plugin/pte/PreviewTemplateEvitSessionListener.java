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
package me.qyh.blog.plugin.pte;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import me.qyh.blog.core.config.Constants;
import me.qyh.blog.template.service.TemplateService;

public class PreviewTemplateEvitSessionListener implements HttpSessionListener {

	private final TemplateService templateService;

	public PreviewTemplateEvitSessionListener(TemplateService templateService) {
		super();
		this.templateService = templateService;
	}

	@Override
	public void sessionCreated(HttpSessionEvent se) {
		//
	}

	@Override
	public void sessionDestroyed(HttpSessionEvent se) {
		HttpSession session = se.getSession();
		if (session.getAttribute(Constants.USER_SESSION_KEY) != null) {
			templateService.clearPreview();
		}
	}

}
