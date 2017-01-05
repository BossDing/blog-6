package me.qyh.blog.util;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Objects;

import me.qyh.blog.entity.DateDeserializer;
import me.qyh.blog.message.Message;

public abstract class TimeDiffParser {

	/**
	 * Minutes per hour.
	 */
	static final int MINUTES_PER_HOUR = 60;
	/**
	 * Seconds per minute.
	 */
	static final int SECONDS_PER_MINUTE = 60;
	/**
	 * Seconds per hour.
	 */
	static final int SECONDS_PER_HOUR = SECONDS_PER_MINUTE * MINUTES_PER_HOUR;
	/**
	 * Seconds per day.
	 */
	static final int SECONDS_PER_DAY = 24 * SECONDS_PER_HOUR;

	public abstract Message parseDiff(LocalDateTime begin, LocalDateTime end);

	public final Message parseDiff(LocalDateTime begin) {
		Objects.requireNonNull(begin, "开始日期不能为空");
		return parseDiff(begin, LocalDateTime.now());
	}

	public final Message parseDiff(Timestamp begin) {
		Objects.requireNonNull(begin, "开始日期不能为空");
		return parseDiff(begin.toLocalDateTime());
	}

	public final Message parseDiff(Date begin) {
		Objects.requireNonNull(begin, "开始日期不能为空");
		return parseDiff(LocalDateTime.ofInstant(begin.toInstant(), ZoneId.systemDefault()));
	}

	public final Message parseDiff(String begin) {
		return parseDiff(begin, null);
	}

	public final Message parseDiff(String begin, String end) {
		Objects.requireNonNull(begin, "开始日期不能为空");
		LocalDateTime beginTime = DateDeserializer.parse(begin);
		if (beginTime == null) {
			return new Message("datetime.parse.fail", "日期" + begin + "解析失败", begin);
		}
		LocalDateTime endTime = LocalDateTime.now();
		if (!Validators.isEmptyOrNull(end, true)) {
			endTime = DateDeserializer.parse(end);
		}
		if (endTime == null) {
			return new Message("datetime.parse.fail", "日期" + end + "解析失败", end);
		}
		return parseDiff(beginTime, endTime);
	}

	public final Message parseDiff(Timestamp begin, Timestamp end) {
		Objects.requireNonNull(begin, "开始日期不能为空");
		Objects.requireNonNull(end, "结束日期不能为空");
		return parseDiff(begin.toLocalDateTime(), end.toLocalDateTime());
	}

	public final Message parseDiff(Date begin, Date end) {
		Objects.requireNonNull(begin, "开始日期不能为空");
		Objects.requireNonNull(end, "结束日期不能为空");
		LocalDateTime beginTime = LocalDateTime.ofInstant(begin.toInstant(), ZoneId.systemDefault());
		LocalDateTime endTime = LocalDateTime.ofInstant(end.toInstant(), ZoneId.systemDefault());
		return parseDiff(beginTime, endTime);
	}

}
