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
 * 
 * @author Administrator
 *
 */
public interface Template {

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
}
