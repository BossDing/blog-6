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

import javax.servlet.http.HttpServletRequest;

import me.qyh.blog.ui.Params;

public interface ExpandedPageHandler {

	/**
	 * 是否能够处理这个请求
	 * 
	 * @param request
	 * @return
	 */
	boolean match(HttpServletRequest request);

	Params fromHttpRequest(HttpServletRequest request);

	/**
	 * 获取页面模板
	 * 
	 * @return
	 */
	String getTemplate();

	/**
	 * 拓展页面的唯一ID
	 * 
	 * @return
	 */
	int id();

	/**
	 * 拓展页面名称(用来在页面显示)
	 * 
	 * @return
	 */
	String name();
}
