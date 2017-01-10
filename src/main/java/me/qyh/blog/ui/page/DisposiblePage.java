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

/**
 * 这是一个单独的页面，用来处理单个fragament的渲染以及页面的预览
 * 
 * @author Administrator
 *
 */
public class DisposiblePage extends Page {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * if true load preview data tag
	 */
	private boolean preview = true;

	public DisposiblePage() {
		super();
	}

	public DisposiblePage(Page page) {
		super(page);
	}

	public DisposiblePage(Page page, boolean preview) {
		super(page);
		this.preview = preview;
	}

	@Override
	public final PageType getType() {
		return PageType.DISPOSIBLE;
	}

	public boolean isPreview() {
		return preview;
	}

	public void setPreview(boolean preview) {
		this.preview = preview;
	}

}
