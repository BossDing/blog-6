package me.qyh.blog.util;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;

import me.qyh.blog.message.Message;
import me.qyh.blog.util.Times;

public abstract class TimeDiffParser {

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
		Optional<LocalDateTime> optionalBeginTime = Times.parse(begin);
		if (!optionalBeginTime.isPresent()) {
			return new Message("datetime.parse.fail", "日期" + begin + "解析失败", begin);
		}
		LocalDateTime endTime = LocalDateTime.now();
		if (!Validators.isEmptyOrNull(end, true)) {
			endTime = Times.parse(end).orElse(null);
		}
		if (endTime == null) {
			return new Message("datetime.parse.fail", "日期" + end + "解析失败", end);
		}
		return parseDiff(optionalBeginTime.get(), endTime);
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
