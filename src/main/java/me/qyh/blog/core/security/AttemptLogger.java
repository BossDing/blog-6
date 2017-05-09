package me.qyh.blog.core.security;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import me.qyh.blog.core.exception.SystemException;

/**
 * 用来判断是否需要输入验证码
 * <p>
 * 当某个ip尝试此时达到attemptCount的情况下，如果该ip继续尝试，则需要输入验证码<br>
 * 当尝试总数达到maxAttemptCount的情况下，如果有任何ip继续尝试，则需要输入验证码<br>
 * </p>
 * 基本用法:
 * <pre>
 * AttemptLogger logger = new AttemptLogger(10,100);
 * if(logger.log(ip)){
 *   //判断验证码是否正确
 * }
 * 
 * reach(ip) 
 * //判断某个ip是否需要输入验证码
 * </pre>
 * 
 * @author Administrator
 *
 */
public class AttemptLogger {

	private final int attemptCount;
	private final int maxAttemptCount;
	private final Map<String, AttemptInfo> map = new ConcurrentHashMap<>();
	private final AtomicInteger maxAttemptCounter;

	public AttemptLogger(int attemptCount, int maxAttemptCount) {
		super();
		if (attemptCount < 1) {
			throw new SystemException("尝试次数不能小于1");
		}
		if (maxAttemptCount < attemptCount) {
			throw new SystemException("总尝试次数不能小于attemptCount");
		}
		this.attemptCount = attemptCount;
		this.maxAttemptCount = maxAttemptCount;
		this.maxAttemptCounter = new AtomicInteger(0);
	}

	/**
	 * 尝试，次数+1
	 * 
	 * @param t
	 * @return 如果返回true，则说明达到阈值
	 */
	public boolean log(String t) {
		Objects.requireNonNull(t);
		BooleanHolder holder = new BooleanHolder(true);
		map.compute(t, (k, v) -> {
			// 尽可能减少创建对象
			if (v == null && maxAttemptCounter.get() < maxAttemptCount) {
				v = new AttemptInfo();
			}
			if (v != null) {
				holder.value = add(v);
			}
			return v;
		});
		return holder.value;
	}

	/**
	 * 是否达到阈值
	 * 
	 * @param t
	 * @return
	 */
	public boolean reach(String t) {
		if (maxAttemptCounter.get() == maxAttemptCount) {
			return true;
		}
		AttemptInfo info = map.get(t);
		return info != null && info.reach();
	}

	private boolean add(AttemptInfo v) {
		// blocking...
		v.lastAttemptTime = System.currentTimeMillis();
		int count = v.getCount();
		if (count == attemptCount) {
			return true;
		}
		// non blocking...
		for (;;) {
			int maxCount = maxAttemptCounter.get();
			if (maxCount == maxAttemptCount) {
				return true;
			}
			if (maxAttemptCounter.compareAndSet(maxCount, maxCount + 1)) {
				v.increment();
				return false;
			}
		}
	}

	/**
	 * 移除一个记录
	 * 
	 * @param t
	 */
	public void remove(String t) {
		map.computeIfPresent(t, (k, v) -> {
			maxAttemptCounter.addAndGet(-v.getCount());
			return null;
		});
	}

	/**
	 * 按条件删除记录
	 * 
	 * @param predicate
	 *            <b>应该简短</b>
	 */
	public void remove(Predicate<AttemptInfo> predicate) {
		map.keySet().forEach(t -> {
			map.computeIfPresent(t, (k, v) -> {
				if (predicate.test(v)) {
					maxAttemptCounter.addAndGet(-v.getCount());
					return null;
				}
				return v;
			});
		});
	}

	public final class AttemptInfo {
		private long lastAttemptTime;
		private AtomicInteger count;

		public AttemptInfo() {
			super();
			this.count = new AtomicInteger(0);
			this.lastAttemptTime = System.currentTimeMillis();
		}

		/**
		 * 获取最后一次尝试的时间
		 * 
		 * @return
		 */
		public long getLastAttemptTime() {
			return lastAttemptTime;
		}

		/**
		 * 是否达到阈值
		 * 
		 * @return
		 */
		public boolean reach() {
			return count.get() == attemptCount;
		}

		private void increment() {
			count.incrementAndGet();
		}

		private int getCount() {
			return count.get();
		}

		@Override
		public String toString() {
			return "AttemptInfo [count=" + count + "]";
		}
	}

	private final class BooleanHolder {
		private boolean value;

		public BooleanHolder(boolean value) {
			super();
			this.value = value;
		}
	}
}
