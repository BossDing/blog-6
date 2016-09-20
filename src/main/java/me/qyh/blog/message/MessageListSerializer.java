package me.qyh.blog.message;

import java.io.IOException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;

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
public class MessageListSerializer extends JsonSerializer<List<Message>> {

	@Autowired
	private Messages messages;

	public MessageListSerializer() {
		SpringBeanAutowiringSupport.processInjectionBasedOnCurrentContext(this);
	}

	@Override
	public void serialize(List<Message> value, JsonGenerator gen, SerializerProvider serializers)
			throws IOException, JsonProcessingException {
		gen.writeStartArray();
		for (Message message : value) {
			gen.writeString(messages.getMessage(message));
		}
		gen.writeEndArray();
	}

}
