package me.qyh.blog.ui.dialect;

import org.springframework.context.ApplicationContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.processor.element.AbstractElementTagProcessor;
import org.thymeleaf.spring4.context.SpringContextUtils;
import org.thymeleaf.templatemode.TemplateMode;

import me.qyh.blog.exception.SystemException;

public abstract class TransactionSupport extends AbstractElementTagProcessor {

	private PlatformTransactionManager transactionManager;

	public TransactionSupport(TemplateMode templateMode, String dialectPrefix, String elementName,
			boolean prefixElementName, String attributeName, boolean prefixAttributeName, int precedence) {
		super(templateMode, dialectPrefix, elementName, prefixElementName, attributeName, prefixAttributeName,
				precedence);
	}

	private void checkTransactionManager(ITemplateContext context) {
		if (transactionManager == null) {
			ApplicationContext ctx = SpringContextUtils.getApplicationContext(context);
			if (ctx != null) {
				transactionManager = ctx.getBean(PlatformTransactionManager.class);
			}
			if (transactionManager == null) {
				throw new SystemException("没有可用的" + PlatformTransactionManager.class.getName());
			}
		}
	}

	protected PlatformTransactionManager getTransactionManager(ITemplateContext context) {
		checkTransactionManager(context);
		return transactionManager;
	}

	protected TransactionStatus getTransactionStatus(ITemplateContext context) {
		checkTransactionManager(context);
		DefaultTransactionDefinition defaultTransactionDefinition = new DefaultTransactionDefinition();
		defaultTransactionDefinition.setReadOnly(true);
		return transactionManager.getTransaction(defaultTransactionDefinition);
	}

}
