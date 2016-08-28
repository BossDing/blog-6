package me.qyh.blog.message;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;
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
public class MessageSerializer extends JsonSerializer<Message> {

	@Autowired
	private Messages messages;

	@Override
	public Class<Message> handledType() {
		return Message.class;
	}

	@Override
	public void serialize(Message value, JsonGenerator gen, SerializerProvider serializers)
			throws IOException, JsonProcessingException {
		String message = HtmlUtils.htmlEscape(messages.getMessage(value));
		gen.writeString(message);
	}

	public MessageSerializer() {
		SpringBeanAutowiringSupport.processInjectionBasedOnCurrentContext(this);
	}

}
