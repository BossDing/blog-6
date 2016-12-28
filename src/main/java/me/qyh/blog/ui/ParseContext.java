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

import org.springframework.transaction.TransactionStatus;

/**
 * 解析上下文
 * 
 * @author mhlx
 *
 */
public class ParseContext {

	private static final ThreadLocal<ParseStatus> statusLocal = new ThreadLocal<>();
	private static final ThreadLocal<TransactionStatus> transactionLocal = new ThreadLocal<>();

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

}
