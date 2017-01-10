package me.qyh.blog.ui.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.Temporal;
import java.util.Objects;
import java.util.Optional;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

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

	private static final LoadingCache<String, DateTimeFormatter> DATE_TIME_FORMATTER_CACHE = CacheBuilder.newBuilder()
			.build(new CacheLoader<String, DateTimeFormatter>() {

				@Override
				public DateTimeFormatter load(String key) throws Exception {
					return DateTimeFormatter.ofPattern(key);
				}

			});

	static {
		DATE_FORMATTERS = new DateTimeFormatter[] { DATE_TIME_FORMATTER_CACHE.getUnchecked("yyyy-MM-dd"),
				DATE_TIME_FORMATTER_CACHE.getUnchecked("yyyy/MM/dd") };
		DATE_TIME_PARSERS = new DateTimeFormatter[] { DATE_TIME_FORMATTER_CACHE.getUnchecked("yyyy-MM-dd HH:mm:ss"),
				DATE_TIME_FORMATTER_CACHE.getUnchecked("yyyy-MM-dd HH:mm"),
				DATE_TIME_FORMATTER_CACHE.getUnchecked("yyyy-MM-dd HH"),
				DATE_TIME_FORMATTER_CACHE.getUnchecked("yyyy/MM/dd HH:mm:ss"),
				DATE_TIME_FORMATTER_CACHE.getUnchecked("yyyy/MM/dd HH:mm"),
				DATE_TIME_FORMATTER_CACHE.getUnchecked("yyyy/MM/dd HH") };
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
	 * 解析失败
	 * 
	 * @param text
	 * @return 如果解析失败，返回null
	 */
	public static LocalDateTime parseAndGet(String text) {
		return parse(text).orElse(null);
	}

	/**
	 * 解析日期
	 * 
	 * @param text
	 * @return
	 */
	public static Optional<LocalDateTime> parse(String text) {
		if (text.indexOf(' ') == -1) {
			for (DateTimeFormatter formatter : DATE_FORMATTERS) {
				// may be date
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
		return DATE_TIME_FORMATTER_CACHE.getUnchecked(pattern).format(temporal);
	}
}
