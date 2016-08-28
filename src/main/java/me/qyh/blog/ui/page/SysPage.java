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
		if (!hasId()) {
			Space space = getSpace();
			return PREFIX + (space == null ? target.name() : space.getAlias() + "-" + target.name());
		}
		return super.getTemplateName();
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

	public static void main(String[] args) {
	}
}
