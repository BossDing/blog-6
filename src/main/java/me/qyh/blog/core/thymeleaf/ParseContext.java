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
package me.qyh.blog.core.thymeleaf;

import org.springframework.transaction.TransactionStatus;

import me.qyh.blog.core.thymeleaf.template.Template;

/**
 * 解析上下文
 * 
 * @author mhlx
 *
 */
public class ParseContext {

	private static final ThreadLocal<TransactionStatus> TRANSACTION_LOCAL = new ThreadLocal<>();
	private static final ThreadLocal<ParseConfig> CONFIG_LOCAL = ThreadLocal.withInitial(ParseConfig::new);
	private static final ThreadLocal<Template> ROOT_LOCAL = new ThreadLocal<>();

	public static void setTransactionStatus(TransactionStatus status) {
		TRANSACTION_LOCAL.set(status);
	}

	public static TransactionStatus getTransactionStatus() {
		return TRANSACTION_LOCAL.get();
	}

	public static void remove() {
		TRANSACTION_LOCAL.remove();
		CONFIG_LOCAL.remove();
		ROOT_LOCAL.remove();
	}

	public static void removeTransactionStatus() {
		TRANSACTION_LOCAL.remove();
	}

	public static boolean onlyCallable() {
		return getConfig().isOnlyCallable();
	}

	public static void setConfig(ParseConfig config) {
		CONFIG_LOCAL.set(config);
	}

	private static ParseConfig getConfig() {
		return CONFIG_LOCAL.get();
	}

	public static Template getRoot() {
		return ROOT_LOCAL.get();
	}

	public static void setRoot(Template template) {
		ROOT_LOCAL.set(template);
	}
}
