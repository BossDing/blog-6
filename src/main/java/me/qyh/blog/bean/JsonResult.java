package me.qyh.blog.bean;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import me.qyh.blog.message.Message;
import me.qyh.blog.message.MessageSerializer;

/**
 * 用于Json结果的返回
 * 
 * @author mhlx
 *
 */
public class JsonResult {

	private boolean success;
	private Object data;

	@JsonSerialize(using = MessageSerializer.class)
	private Message message;

	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public Object getData() {
		return data;
	}

	public void setData(Object data) {
		this.data = data;
	}

	public JsonResult(boolean success, Object data) {
		this.success = success;
		this.data = data;
	}

	public JsonResult(boolean success, Message message) {
		this.success = success;
		this.message = message;
	}

	public JsonResult(boolean success) {
		this.success = success;
	}

	public Message getMessage() {
		return message;
	}

	public void setMessage(Message message) {
		this.message = message;
	}

}
