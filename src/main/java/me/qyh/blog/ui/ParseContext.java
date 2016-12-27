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
