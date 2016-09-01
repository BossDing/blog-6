package me.qyh.blog.config;

import java.util.concurrent.TimeUnit;

/**
 * 在指定时间内最多能执行limit次操作
 * 
 * @author Administrator
 *
 */
public class Limit {

	private int limit;
	private long time;
	private TimeUnit unit;

	public int getLimit() {
		return limit;
	}

	public void setLimit(int limit) {
		this.limit = limit;
	}

	public long getTime() {
		return time;
	}

	public void setTime(long time) {
		this.time = time;
	}

	public TimeUnit getUnit() {
		return unit;
	}

	public void setUnit(TimeUnit unit) {
		this.unit = unit;
	}

	public TimePeriod getPeriod() {
		long end = System.currentTimeMillis();
		long start = System.currentTimeMillis() - unit.toMillis(time);
		return new TimePeriod(start, end);
	}

	public final class TimePeriod {
		private long start;
		private long end;

		public long getStart() {
			return start;
		}

		public long getEnd() {
			return end;
		}

		private TimePeriod(long start, long end) {
			super();
			this.start = start;
			this.end = end;
		}

	}

}
