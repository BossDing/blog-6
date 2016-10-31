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

	public Limit() {

	}

	public Limit(int limit, long time, TimeUnit unit) {
		this.limit = limit;
		this.time = time;
		this.unit = unit;
	}

	public long toMill() {
		return unit.toMillis(time);
	}
}
