package me.qyh.blog.ui;

public class UIContext {

	private static ThreadLocal<Template> templateLocal = new ThreadLocal<Template>();

	public static void set(Template template) {
		templateLocal.set(template);
	}

	public static Template get() {
		return templateLocal.get();
	}

	public static void remove() {
		templateLocal.remove();
	}
}
