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
