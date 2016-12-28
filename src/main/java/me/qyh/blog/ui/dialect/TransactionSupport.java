/*
 * Copyright 2016 qyh.me
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
