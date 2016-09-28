package me.qyh.blog.ui;

import java.util.HashSet;
import java.util.Set;

import org.thymeleaf.dialect.AbstractProcessorDialect;
import org.thymeleaf.processor.IProcessor;
import org.thymeleaf.standard.StandardDialect;

public class PageDialect extends AbstractProcessorDialect {

	private static final String DIALECT_NAME = "Page Dialect";

	public PageDialect() {
		super(DIALECT_NAME, "page", StandardDialect.PROCESSOR_PRECEDENCE);
	}

	public Set<IProcessor> getProcessors(final String dialectPrefix) {
		final Set<IProcessor> processors = new HashSet<IProcessor>();
		processors.add(new DataTagProcessor(dialectPrefix));
		processors.add(new FragementTagProcessor(dialectPrefix));
		return processors;
	}

}