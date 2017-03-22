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
package me.qyh.blog.core.ui.page;

import me.qyh.blog.core.entity.Space;
import me.qyh.blog.core.message.Message;
import me.qyh.blog.core.ui.Template;
import me.qyh.blog.core.ui.TemplateUtils;

public class SysPage extends Page {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public enum PageTarget {
		INDEX(new Message("page.index", "首页")), // 首页
		ARTICLE_DETAIL(new Message("page.articleDetail", "文章明细页")), // 博客明细页
		ERROR(new Message("page.error", "错误显示页面"));
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

	public SysPage(PageTarget target) {
		this.target = target;
	}

	public SysPage(SysPage page) {
		super(page);
		this.target = page.target;
	}

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

	public Page toExportPage() {
		SysPage page = new SysPage();
		page.setTpl(getTpl());
		page.setType(PageType.SYSTEM);
		page.setTarget(target);
		return page;
	}

	@Override
	public String toString() {
		return "SysPage [target=" + target + "]";
	}

	public String getTemplateName() {
		return TemplateUtils.getTemplateName(this);
	}

	@Override
	public Template cloneTemplate() {
		return new SysPage(this);
	}
}
