package me.qyh.blog.ui;

import me.qyh.blog.ui.page.Page;

public class UIContext {

	private static ThreadLocal<Page> pageLocal = new ThreadLocal<Page>();

	public static void set(Page page) {
		pageLocal.set(page);
	}

	public static Page get() {
		return pageLocal.get();
	}

	public static void remove() {
		pageLocal.remove();
	}
}
