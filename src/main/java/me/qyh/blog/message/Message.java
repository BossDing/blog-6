package me.qyh.blog.message;

import org.springframework.context.MessageSourceResolvable;

/**
 * 用于Json结果的返回
 * 
 * @author mhlx
 *
 */
public class Message implements MessageSourceResolvable {

	private String code;
	private Object[] arguments;
	private String defaultMessage;

	public Message(String code, String defaultMessage, Object... arguments) {
		this.code = code;
		this.arguments = arguments;
		this.defaultMessage = defaultMessage;
	}

	public Message(String code) {
		this.code = code;
	}

	public String getDefaultMessage() {
		return defaultMessage;
	}

	@Override
	public String[] getCodes() {
		return new String[] { code };
	}

	@Override
	public Object[] getArguments() {
		return arguments;
	}

}
