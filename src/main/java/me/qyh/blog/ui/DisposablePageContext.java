package me.qyh.blog.ui;

import me.qyh.blog.ui.page.DisposiblePage;

public final class DisposablePageContext {

	private static final ThreadLocal<DisposiblePage> disposablePageLocal = new ThreadLocal<>();

	public static DisposiblePage get() {
		return disposablePageLocal.get();
	}

	public static void set(DisposiblePage value) {
		disposablePageLocal.set(value);
	}

	public static void clear() {
		disposablePageLocal.remove();
	}

}
