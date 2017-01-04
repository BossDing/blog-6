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
package me.qyh.blog.entity;

import java.lang.reflect.Type;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

/**
 * JsonFormat annotation可以解决同样的问题，但只能局限一种格式？
 * 
 * @author Administrator
 *
 */
public class DateDeserializer implements JsonDeserializer<Timestamp> {

	/**
	 * update to java8
	 */
	private static final DateTimeFormatter[] PARSERS = { DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"), DateTimeFormatter.ofPattern("yyyy-MM-dd HH"),
			DateTimeFormatter.ofPattern("yyyy-MM-dd") };

	@Override
	public Timestamp deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
			throws JsonParseException {
		return parse(json.getAsString().trim());
	}

	private Timestamp parse(String str) throws DateParseProcessingException {
		for (DateTimeFormatter formatter : PARSERS) {
			try {
				return Timestamp.valueOf(LocalDateTime.parse(str, formatter));
			} catch (DateTimeParseException e) {
				continue;
			}
		}
		throw new DateParseProcessingException(str + "无法转化为符合格式的日期");
	}

	private final class DateParseProcessingException extends JsonParseException {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		protected DateParseProcessingException(String msg) {
			super(msg);
		}
	}
}
