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
package me.qyh.blog.core.thymeleaf.template;

import java.io.IOException;

/**
 * 模板
 * 
 * @author Administrator
 *
 */
public interface Template {

	/**
	 * 模板分割符
	 */
	static final String SPLITER = "%";

	/**
	 * 模板前缀，所有的模板名必须以这个开头
	 */
	static final String TEMPLATE_PREFIX = "Template" + SPLITER;

	static boolean isTemplate(String templateName) {
		return templateName != null && templateName.startsWith(TEMPLATE_PREFIX);
	}

	/**
	 * 预览模板路径
	 */
	static final String PREVIEW = TEMPLATE_PREFIX + "Preview";

	/**
	 * 判断页面是否预览模板
	 * 
	 * @param templateName
	 * @return
	 */
	public static boolean isPreview(String templateName) {
		return PREVIEW.equals(templateName);
	}

	/**
	 * 是否是根模板
	 * <p>
	 * 在一次解析中，根模板只能被解析一次
	 * </p>
	 * 
	 * @return
	 */
	boolean isRoot();

	/**
	 * 获取模板内容
	 * 
	 * @return
	 * @throws IOException
	 */
	String getTemplate() throws IOException;

	/**
	 * 获取模板名称
	 * <p>
	 * 模板名称应该是全局唯一的
	 * </p>
	 * 
	 * @return
	 */
	String getTemplateName();

	/**
	 * 克隆 template
	 * 
	 * @return
	 */
	Template cloneTemplate();

	/**
	 * 是否可被外部调用
	 * 
	 * @return
	 */
	boolean isCallable();

	/**
	 * 判断是否和另一个Template等价
	 * 
	 * @return
	 */
	boolean equalsTo(Template other);

	/**
	 * 清除模板内容
	 */
	void clearTemplate();
}
