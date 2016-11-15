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
package me.qyh.blog.bean;

import me.qyh.blog.ui.ExportPage;

/**
 * 导入页面封装
 * 
 * @author Administrator
 *
 */
public class ImportPageWrapper {

	private int index;// 页面序号
	private ExportPage page;// 页面

	/**
	 * 构造器
	 * 
	 * @param index
	 *            序号
	 * @param page
	 *            页面
	 */
	public ImportPageWrapper(int index, ExportPage page) {
		super();
		this.index = index;
		this.page = page;
	}

	public int getIndex() {
		return index;
	}

	public ExportPage getPage() {
		return page;
	}

}
