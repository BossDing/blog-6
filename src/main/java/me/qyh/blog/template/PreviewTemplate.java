/*
 * Copyright 2018 qyh.me
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

public final class PreviewTemplate implements Template {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final Template template;

	public Template getOriginalTemplate() {
		return template;
	}

	public PreviewTemplate(Template template) {
		super();
		this.template = template;
	}

	@Override
	public boolean isRoot() {
		return template.isRoot();
	}

	@Override
	public String getTemplate() {
		return template.getTemplate();
	}

	@Override
	public String getTemplateName() {
		return Template.TEMPLATE_PREVIEW_PREFIX + template.getTemplateName();
	}

	@Override
	public Template cloneTemplate() {
		return new PreviewTemplate(template);
	}

	@Override
	public boolean isCallable() {
		return template.isCallable();
	}

	@Override
	public boolean equalsTo(Template other) {
		return false;
	}
}