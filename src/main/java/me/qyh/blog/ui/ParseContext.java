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
package me.qyh.blog.ui;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.transaction.TransactionStatus;

import me.qyh.blog.ui.fragment.Fragment;
import me.qyh.blog.ui.page.Page;

/**
 * 解析上下文
 * 
 * @author mhlx
 *
 */
public class ParseContext {

	private static final ThreadLocal<Boolean> startLocal = new ThreadLocal<>();
	private static final ThreadLocal<TransactionStatus> transactionLocal = new ThreadLocal<>();
	private static final ThreadLocal<Page> pageLocal = new ThreadLocal<>();
	private static final ThreadLocal<ParseConfig> configLocal = ThreadLocal.withInitial(ParseConfig::new);
	private static final ThreadLocal<Map<String, Fragment>> fragmentsLocal = ThreadLocal.withInitial(HashMap::new);

	public static void setTransactionStatus(TransactionStatus status) {
		transactionLocal.set(status);
	}

	public static TransactionStatus getTransactionStatus() {
		return transactionLocal.get();
	}

	public static void remove() {
		startLocal.remove();
		transactionLocal.remove();
		pageLocal.remove();
		configLocal.remove();
		fragmentsLocal.remove();
	}

	public static void start() {
		startLocal.set(Boolean.TRUE);
	}

	public static boolean isStart() {
		Boolean start = startLocal.get();
		return start != null;
	}

	public static void removeTransactionStatus() {
		transactionLocal.remove();
	}

	public static boolean isPreview() {
		return getConfig().isPreview();
	}

	public static boolean onlyCallable() {
		return getConfig().isOnlyCallable();
	}

	public static boolean isDisposible() {
		return getConfig().isDisposible();
	}

	public static Page getPage() {
		return pageLocal.get();
	}

	public static void setConfig(ParseConfig config) {
		configLocal.set(config);
	}

	public static void setPage(Page page) {
		pageLocal.set(page);
	}

	public static void addFragment(Fragment fragment) {
		fragmentsLocal.get().put(TemplateUtils.getTemplateName(fragment), fragment);
	}

	public static Optional<Fragment> getFragment(String templateName) {
		return Optional.ofNullable(fragmentsLocal.get().get(templateName));
	}

	private static ParseConfig getConfig() {
		return configLocal.get();
	}
}
