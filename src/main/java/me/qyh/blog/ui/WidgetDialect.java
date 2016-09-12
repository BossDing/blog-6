package me.qyh.blog.ui;

import java.util.HashSet;
import java.util.Set;

import org.thymeleaf.dialect.AbstractProcessorDialect;
import org.thymeleaf.processor.IProcessor;
import org.thymeleaf.standard.StandardDialect;

public class WidgetDialect extends AbstractProcessorDialect {

	private static final String DIALECT_NAME = "Widget Dialect";

	public WidgetDialect() {
		super(DIALECT_NAME, "widget", StandardDialect.PROCESSOR_PRECEDENCE);
	}

	public Set<IProcessor> getProcessors(final String dialectPrefix) {
		final Set<IProcessor> processors = new HashSet<IProcessor>();
		processors.add(new WidgetTagProcessor(dialectPrefix));
		return processors;
	}

}