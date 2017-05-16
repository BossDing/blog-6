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
package me.qyh.blog.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.Temporal;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;

import me.qyh.blog.core.exception.SystemException;

/**
 * java8 date utils for thymeleaf
 * 
 * @author Administrator
 *
 */
@UIUtils
public class Times {

	private static final DateTimeFormatter[] DATE_TIME_PARSERS;
	private static final DateTimeFormatter[] DATE_FORMATTERS;

	private static final LoadingCache<String, DateTimeFormatter> DATE_TIME_FORMATTER_CACHE = Caffeine.newBuilder()
			.build(new CacheLoader<String, DateTimeFormatter>() {

				@Override
				public DateTimeFormatter load(String key) throws Exception {
					return DateTimeFormatter.ofPattern(key);
				}

			});

	static {
		DATE_FORMATTERS = new DateTimeFormatter[] { DATE_TIME_FORMATTER_CACHE.get("yyyy-MM-dd"),
				DATE_TIME_FORMATTER_CACHE.get("yyyy/MM/dd") };
		DATE_TIME_PARSERS = new DateTimeFormatter[] { DATE_TIME_FORMATTER_CACHE.get("yyyy-MM-dd HH:mm:ss"),
				DATE_TIME_FORMATTER_CACHE.get("yyyy-MM-dd HH:mm"), DATE_TIME_FORMATTER_CACHE.get("yyyy-MM-dd HH"),
				DATE_TIME_FORMATTER_CACHE.get("yyyy/MM/dd HH:mm:ss"), DATE_TIME_FORMATTER_CACHE.get("yyyy/MM/dd HH:mm"),
				DATE_TIME_FORMATTER_CACHE.get("yyyy/MM/dd HH") };
	}

	private Times() {
		super();
	}

	/**
	 * 获取现在的日期
	 * 
	 * @return
	 */
	public static LocalDateTime now() {
		return LocalDateTime.now();
	}

	/**
	 * 解析日期
	 * 
	 * @param text
	 * @return 如果解析失败，返回null
	 */
	public static LocalDateTime parseAndGet(String text) {
		return parse(text).orElse(null);
	}

	/**
	 * 通过指定的pattern解析日期
	 * 
	 * @param text
	 * @param pattern
	 * @return
	 */
	public static Optional<LocalDateTime> parse(String text, String pattern) {
		try {
			return Optional.of(LocalDateTime.parse(text, DATE_TIME_FORMATTER_CACHE.get(pattern)));
		} catch (DateTimeParseException e) {
			return Optional.empty();
		}
	}

	/**
	 * 解析日期
	 * 
	 * @param text
	 * @return
	 */
	public static Optional<LocalDateTime> parse(String text) {
		if (text.indexOf(' ') == -1) {
			// may be date
			for (DateTimeFormatter formatter : DATE_FORMATTERS) {
				try {
					return Optional.of(LocalDateTime.from(LocalDate.parse(text, formatter).atStartOfDay()));
				} catch (DateTimeParseException e) {
					continue;
				}
			}
			return Optional.empty();
		}
		for (DateTimeFormatter formatter : DATE_TIME_PARSERS) {
			try {
				return Optional.of(LocalDateTime.parse(text, formatter));
			} catch (DateTimeParseException e) {
				continue;
			}
		}
		return Optional.empty();
	}

	/**
	 * 格式化日期
	 * 
	 * @param localDateTime
	 *            日期
	 * @return pattern
	 */
	public static String format(Temporal temporal, String pattern) {
		Objects.requireNonNull(temporal);
		Objects.requireNonNull(pattern);
		DateTimeFormatter dtf = DATE_TIME_FORMATTER_CACHE.get(pattern);
		if (dtf == null) {
			throw new SystemException("无法获取" + pattern + "对应的DateTimeFormatter");
		}
		return dtf.format(temporal);
	}

	/**
	 * 格式化日期
	 * 
	 * @param localDateTime
	 *            日期
	 * @return pattern
	 */
	public static String format(Date date, String pattern) {
		Objects.requireNonNull(date);
		return format(toLocalDateTime(date), pattern);
	}

	/**
	 * 将Date转化为LocalDateTime
	 * 
	 * @param date
	 * @return
	 */
	public static LocalDateTime toLocalDateTime(Date date) {
		Objects.requireNonNull(date);
		return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
	}

	/**
	 * 解析日期
	 * 
	 * @param text
	 * @return 如果解析失败，返回null
	 */
	public static Date parseAndGetDate(String text) {
		Optional<LocalDateTime> time = parse(text);
		return time.isPresent() ? toDate(time.get()) : null;
	}

	/**
	 * 将LocalDateTime转化为date
	 * 
	 * @param time
	 * @return
	 */
	public static Date toDate(LocalDateTime time) {
		Objects.requireNonNull(time);
		return Date.from(time.atZone(ZoneId.systemDefault()).toInstant());
	}
}
