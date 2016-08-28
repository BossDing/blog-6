package me.qyh.blog.input;

import java.io.IOException;

import org.springframework.web.util.HtmlUtils;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

/**
 * 对对象的string类型属性进行标签转化
 * 
 * @author mhlx
 *
 */
public class JsonHtmlXssSerializer extends JsonSerializer<String> {

	@Override
	public void serialize(String value, JsonGenerator jsonGenerator, SerializerProvider arg2)
			throws IOException, JsonProcessingException {
		if (value != null) {
			String encodedValue = HtmlUtils.htmlEscape(value);
			jsonGenerator.writeString(encodedValue);
		}
	}

	@Override
	public Class<String> handledType() {
		return String.class;
	}

}
