package me.qyh.blog.ui;

import org.thymeleaf.spring4.SpringTemplateEngine;

public class UITemplateEngine extends SpringTemplateEngine {

	public UITemplateEngine() {
		super();
		addDialect(new WidgetDialect());
	}

}
