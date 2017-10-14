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
package me.qyh.blog.template;

import javax.servlet.http.HttpServletRequest;

import me.qyh.blog.web.view.TemplateView;

/**
 * 将路径模板转化为一个Controller
 * 
 * @author mhlx
 *
 */
public abstract class TemplateController {

	private final TemplateView templateView;
	private final String templateName;
	private final String path;
	private final Integer id;

	protected TemplateController(Integer id, String templateName, String path) {
		super();
		this.templateView = new TemplateView(templateName);
		this.templateName = templateName;
		this.path = path;
		this.id = id;
	}

	public abstract TemplateView handleRequest(HttpServletRequest request);

	/**
	 * 返回模板ID，id越大，优先级越高
	 * 
	 * @return
	 */
	public Integer getId() {
		return id;
	}

	public TemplateView getTemplateView() {
		return templateView;
	}

	public String getTemplateName() {
		return templateName;
	}

	public String getPath() {
		return path;
	}
}
