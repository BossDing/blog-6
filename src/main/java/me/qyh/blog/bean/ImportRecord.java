package me.qyh.blog.bean;

import me.qyh.blog.message.Message;

public class ImportRecord {
	private final boolean success;
	private final Message message;

	public ImportRecord(boolean success, Message message) {
		super();
		this.success = success;
		this.message = message;
	}

	public boolean isSuccess() {
		return success;
	}

	public Message getMessage() {
		return message;
	}

}
