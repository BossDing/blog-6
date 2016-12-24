package me.qyh.blog.ui.dialect;

import org.springframework.transaction.TransactionStatus;

public class TransactionContext {

	private static final ThreadLocal<TransactionStatus> transactionLocal = new ThreadLocal<>();

	public static void set(TransactionStatus status) {
		transactionLocal.set(status);
	}

	public static TransactionStatus get() {
		return transactionLocal.get();
	}

	public static void remove() {
		transactionLocal.remove();
	}

}
