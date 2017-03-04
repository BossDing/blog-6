package me.qyh.blog.service.impl;

import java.util.Objects;
import java.util.function.Consumer;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 用来处理一些事务<b>事务提交</b>之后的事情
 * <p>
 * 比如事务发布，<i>虽然spring4.2+提供了TransactionEventListener，但匿名注入的bean并不会触发，这样做为了更小的代码改动</i>
 * </p>
 * <p>
 * <b>这个方法必须在事务中使用，eg</b>
 * 
 * <pre>
 * TransactionStatus status = begin();
 * Transactions.afterCommit(() -&gt; System.out.println("commit"));
 * commit(status);
 * </pre>
 * </p>
 * 
 * @see TransactionSynchronizationManager
 * @author Administrator
 *
 */
public final class Transactions {

	private Transactions() {
		super();
	}

	/**
	 * 事务提交成功后回调
	 * 
	 * @param callback
	 */
	public static void afterCommit(Callback callback) {
		Objects.requireNonNull(callback);
		afterCompletion((status) -> {
			if (status == TransactionSynchronization.STATUS_COMMITTED) {
				callback.callback();
			}
		});
	}

	/**
	 * 事务回滚后回调
	 * 
	 * @param callback
	 */
	public static void afterRollback(Callback callback) {
		Objects.requireNonNull(callback);
		afterCompletion((status) -> {
			if (status == TransactionSynchronization.STATUS_ROLLED_BACK) {
				callback.callback();
			}
		});
	}

	public static void afterCompletion(Consumer<Integer> consumer) {
		Objects.requireNonNull(consumer);
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {

			@Override
			public void afterCompletion(int status) {
				consumer.accept(status);
			}

		});
	}

	@FunctionalInterface
	public interface Callback {
		void callback();
	}

}
