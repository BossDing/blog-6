package me.qyh.blog.ui.dialect;

import java.util.Set;

import org.thymeleaf.dialect.AbstractProcessorDialect;
import org.thymeleaf.processor.IProcessor;

import com.google.common.collect.ImmutableSet;

public class TransactionDialect extends AbstractProcessorDialect {

	public TransactionDialect() {
		super("Transaction Dialect", "transaction", 1);
	}

	@Override
	public Set<IProcessor> getProcessors(String dialectPrefix) {
		return ImmutableSet.of(new TransactionBeginTagProcessor(dialectPrefix),
				new TransactionEndTagProcessor(dialectPrefix));
	}

}
