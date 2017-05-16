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

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

import me.qyh.blog.core.exception.SystemException;
import me.qyh.blog.core.message.Message;

public class DefaultTimeDiffParser extends TimeDiffParser {

	private static final ChronoUnit[] UNITS = { ChronoUnit.YEARS, ChronoUnit.MONTHS, ChronoUnit.DAYS, ChronoUnit.HOURS,
			ChronoUnit.MINUTES, ChronoUnit.SECONDS };

	@Override
	public Message parseDiff(LocalDateTime begin, LocalDateTime end) {
		Objects.requireNonNull(begin, "开始日期不能为空");
		Objects.requireNonNull(end, "结束日期不能为空");
		if (begin.isEqual(end)) {
			return new Message("diff.now", "刚刚");
		}
		for (ChronoUnit unit : UNITS) {
			long diff = unit.between(begin, end);
			if (diff != 0) {
				return toMessage(unit, diff);
			}
		}
		return new Message("diff.now", "刚刚");
	}

	private Message toMessage(ChronoUnit unit, long diff) {
		switch (unit) {
		case YEARS:
			return diff < 0 ? new Message("diff.year.after", -diff + "年后", -diff)
					: new Message("diff.year.before", diff + "年前", diff);
		case MONTHS:
			return diff < 0 ? new Message("diff.month.after", -diff + "个月后", -diff)
					: new Message("diff.month.before", diff + "个月前", diff);
		case DAYS:
			return diff < 0 ? new Message("diff.day.after", -diff + "天后", -diff)
					: new Message("diff.day.before", diff + "天前", diff);
		case HOURS:
			return diff < 0 ? new Message("diff.hour.after", -diff + "小时后", -diff)
					: new Message("diff.hour.before", diff + "小时前", diff);
		case MINUTES:
			return diff < 0 ? new Message("diff.minute.after", -diff + "分钟后", -diff)
					: new Message("diff.minute.before", diff + "分钟前", diff);
		case SECONDS:
			return diff < 0 ? new Message("diff.second.after", -diff + "秒后", -diff)
					: new Message("diff.second.before", diff + "秒前", diff);
		default:
			throw new SystemException("无法判断的ChronoUnit" + unit.name());
		}
	}

}
