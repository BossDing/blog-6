package me.qyh.blog.security;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * 用来监视用户一段时间内的非法点击次数，如果达到指定次数，将其放入禁止名单中
 * 
 * @author mhlx
 *
 * @param <T>
 */
public class InvalidCountMonitor<T> implements InitializingBean {

	/**
	 * 如果在invalidLimitSecond非法点击(请求数)达到了invalidLimitCount,那么请求者将会被放入invalidMap中
	 */
	private final int invalidLimitSecond;
	private final int invalidLimitCount;
	/**
	 * 每隔多少秒清空invalidMap中过期的对象
	 */
	private final int invalidMapClearSec;

	/**
	 * 每隔多少秒清空invalidCountMap中过期的非法点击数
	 */
	private final int invalidCountMapClearSec;

	/**
	 * 非法请求用户被禁时间(秒)
	 */
	private final int invalidSecond;

	private final InvalidMap invalidMap;
	private final InvalidCountMap invalidCountMap;

	@Autowired
	private ThreadPoolTaskScheduler threadPoolTaskScheduler;

	public InvalidCountMonitor(int invalidLimitSecond, int invalidLimitCount, int invalidSecond, int invalidMapClearSec,
			int invalidCountMapClearSec) {
		super();
		this.invalidLimitSecond = invalidLimitSecond;
		this.invalidLimitCount = invalidLimitCount;
		this.invalidMapClearSec = invalidMapClearSec;
		this.invalidCountMapClearSec = invalidCountMapClearSec;
		this.invalidSecond = invalidSecond;
		this.invalidMap = new InvalidMap();
		this.invalidCountMap = new InvalidCountMap();
	}

	public final boolean isInvalid(T t) {
		Long start = invalidMap.get(t);
		if (start != null) {
			if ((System.currentTimeMillis() - start) <= (invalidSecond * 1000L))
				return true;
			invalidMap.remove(t);
		}
		return false;
	}

	public final void increase(T t, long time) {
		invalidCountMap.increase(t, time);
	}

	private final class InvalidMap {
		private final ConcurrentHashMap<T, Long> map;

		public InvalidMap() {
			map = new ConcurrentHashMap<>();
		}

		public void put(T t, long time) {
			map.computeIfAbsent(t, k -> time);
		}

		public void remove(T t) {
			map.remove(t);
		}

		public void removeOvertimes() {
			map.values().removeIf(x -> (x != null && ((System.currentTimeMillis() - x) > invalidSecond * 1000L)));
		}

		public Long get(T t) {
			return map.get(t);
		}
	}

	private final class InvalidCount {
		private long start;
		private AtomicInteger count;

		public InvalidCount(long start) {
			this.start = start;
			this.count = new AtomicInteger(0);
		}

		public boolean overtime(long now) {
			return (now - start) > (invalidLimitSecond * 1000L);
		}

		public int increase() {
			return count.incrementAndGet();
		}

		public InvalidCount(long start, int count) {
			this.start = start;
			this.count = new AtomicInteger(count);
		}
	}

	private final class InvalidCountMap {
		private final ConcurrentHashMap<T, InvalidCount> map;

		public InvalidCountMap() {
			map = new ConcurrentHashMap<>();
		}

		public void put(T t, InvalidCount count) {
			map.computeIfAbsent(t, k -> count);
		}

		public void remove(T t) {
			map.remove(t);
		}

		public void increase(T t, long now) {
			InvalidCount oldCount = map.computeIfAbsent(t, k -> new InvalidCount(now));
			int count = oldCount.increase();
			if (!oldCount.overtime(now) && (count >= invalidLimitCount)) {
				invalidMap.put(t, now);
				invalidCountMap.remove(t);
			} else if (oldCount.overtime(now))
				invalidCountMap.put(t, new InvalidCount(now, 1));
		}

		public void removeOvertimes() {
			map.values().removeIf(x -> (x != null && x.overtime(System.currentTimeMillis())));
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (invalidCountMapClearSec == invalidMapClearSec) {
			threadPoolTaskScheduler.scheduleAtFixedRate(() -> {
				invalidCountMap.removeOvertimes();
				invalidMap.removeOvertimes();
			}, invalidCountMapClearSec * 1000L);
		} else {
			threadPoolTaskScheduler.scheduleAtFixedRate(() -> {
				invalidCountMap.removeOvertimes();
			}, invalidCountMapClearSec * 1000L);

			threadPoolTaskScheduler.scheduleAtFixedRate(() -> {
				invalidMap.removeOvertimes();
			}, invalidMapClearSec * 1000L);
		}
	}
}
