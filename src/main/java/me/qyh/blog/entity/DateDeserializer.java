package me.qyh.blog.entity;

import java.io.IOException;
import java.sql.Timestamp;
import java.text.ParseException;

import org.apache.commons.lang3.time.DateParser;
import org.apache.commons.lang3.time.FastDateFormat;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

/**
 * JsonFormat annotation可以解决同样的问题，但只能局限一种格式？
 * 
 * @author Administrator
 *
 */
public class DateDeserializer extends JsonDeserializer<Timestamp> {

	public static final DateParser[] PARSERS = { FastDateFormat.getInstance("yyyy-MM-dd HH:mm:ss"),
			FastDateFormat.getInstance("yyyy-MM-dd HH:mm"), FastDateFormat.getInstance("yyyy-MM-dd HH"),
			FastDateFormat.getInstance("yyyy-MM-dd") };

	@Override
	public Timestamp deserialize(JsonParser p, DeserializationContext ctxt)
			throws IOException, JsonProcessingException {
		return parse(p.getText().trim());
	}

	private Timestamp parse(String str) throws DateParseProcessingException {
		for (DateParser dp : PARSERS)
			try {
				return new Timestamp(dp.parse(str).getTime());
			} catch (ParseException e) {
				continue;
			}
		throw new DateParseProcessingException(str + "无法转化为符合格式的日期");
	}

	private final class DateParseProcessingException extends JsonProcessingException {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		protected DateParseProcessingException(String msg) {
			super(msg);
		}

	}

}
