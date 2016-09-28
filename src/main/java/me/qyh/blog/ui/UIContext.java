package me.qyh.blog.ui;

public class UIContext {

	private static ThreadLocal<RenderedPage> pageLocal = new ThreadLocal<RenderedPage>();

	public static void set(RenderedPage page) {
		pageLocal.set(page);
	}

	public static RenderedPage get() {
		return pageLocal.get();
	}

	public static void remove() {
		pageLocal.remove();
	}
}
