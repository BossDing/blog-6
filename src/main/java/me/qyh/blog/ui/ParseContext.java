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

import java.util.Map;
import java.util.Optional;

import org.springframework.transaction.TransactionStatus;

import com.google.common.collect.Maps;

import me.qyh.blog.ui.fragment.Fragment;
import me.qyh.blog.ui.page.Page;

/**
 * 解析上下文
 * 
 * @author mhlx
 *
 */
public class ParseContext {

	private static final ThreadLocal<ParseStatus> statusLocal = new ThreadLocal<>();
	private static final ThreadLocal<TransactionStatus> transactionLocal = new ThreadLocal<>();
	private static final ThreadLocal<Page> pageLocal = new ThreadLocal<>();
	private static final ThreadLocal<ParseConfig> configLocal = new ThreadLocal<>();
	private static final ThreadLocal<Map<String, Fragment>> fragmentsLocal = new ThreadLocal<Map<String, Fragment>>() {

		@Override
		protected Map<String, Fragment> initialValue() {
			return Maps.newHashMap();
		}

	};

	public static final ParseConfig DEFAULT_CONFIG = new ParseConfig();

	public enum ParseStatus {
		START, COMPLETE, BREAK;
	}

	public static void setTransactionStatus(TransactionStatus status) {
		transactionLocal.set(status);
	}

	public static TransactionStatus getTransactionStatus() {
		return transactionLocal.get();
	}

	public static void remove() {
		statusLocal.remove();
		transactionLocal.remove();
		pageLocal.remove();
		configLocal.remove();
		fragmentsLocal.remove();
	}

	public static ParseStatus getStatus() {
		return statusLocal.get();
	}

	public static void setStatus(ParseStatus status) {
		statusLocal.set(status);
	}

	public static void start() {
		statusLocal.set(ParseStatus.START);
	}

	public static boolean isStart() {
		ParseStatus status = statusLocal.get();
		return status != null && status.equals(ParseStatus.START);
	}

	public static void removeTransactionStatus() {
		transactionLocal.remove();
	}

	public static boolean isPreview() {
		return getConfig().preview;
	}

	public static boolean onlyCallable() {
		return getConfig().onlyCallable;
	}

	public static boolean isDisposible() {
		return getConfig().disposible;
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
		ParseConfig inLocal = configLocal.get();
		return inLocal == null ? DEFAULT_CONFIG : inLocal;
	}

	public static final class ParseConfig {
		private final boolean preview;
		private final boolean onlyCallable;
		private final boolean disposible;

		public ParseConfig(boolean preview, boolean onlyCallable, boolean disposible) {
			super();
			this.preview = preview;
			this.onlyCallable = onlyCallable;
			this.disposible = disposible;
		}

		public ParseConfig() {
			this(false, false, false);
		}
	}

}
