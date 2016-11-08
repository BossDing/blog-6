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

import me.qyh.blog.entity.Space;
import me.qyh.blog.message.Message;

public class SysPage extends Page {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public enum PageTarget {
		INDEX(new Message("page.index", "首页")), // 首页
		ARTICLE_LIST(new Message("page.articleList", "文章列表页")), // 博客列表页
		ARTICLE_DETAIL(new Message("page.articleDetail", "文章明细页")); // 博客明细页

		private Message message;

		private PageTarget(Message message) {
			this.message = message;
		}

		private PageTarget() {

		}

		public Message getMessage() {
			return message;
		}
	}

	private PageTarget target;

	public PageTarget getTarget() {
		return target;
	}

	public void setTarget(PageTarget target) {
		this.target = target;
	}

	@Override
	public final PageType getType() {
		return PageType.SYSTEM;
	}

	@Override
	public String getTemplateName() {
		Space space = getSpace();
		return PREFIX + "SysPage:" + (space == null ? target.name() : space.getAlias() + "-" + target.name());
	}

	public SysPage() {
		super();
	}

	public SysPage(Integer id) {
		super(id);
	}

	public SysPage(Space space, PageTarget target) {
		super(space);
		this.target = target;
	}

	public Page toExportPage() {
		SysPage page = new SysPage();
		page.setTpl(getTpl());
		page.setType(PageType.SYSTEM);
		page.setTarget(target);
		return page;
	}
}
