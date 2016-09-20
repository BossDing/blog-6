package me.qyh.blog.bean;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import me.qyh.blog.message.Message;
import me.qyh.blog.message.MessageSerializer;

public class ImportError implements Comparable<ImportError> {

	private int index;
	@JsonSerialize(using = MessageSerializer.class)
	private Message message;

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	public Message getMessage() {
		return message;
	}

	public void setMessage(Message message) {
		this.message = message;
	}

	public ImportError() {

	}

	public ImportError(int index, Message message) {
		this.index = index;
		this.message = message;
	}

	@Override
	public int compareTo(ImportError o) {
		return index < o.index ? 1 : (index == o.index) ? 0 : -1;
	}

}
