package me.qyh.blog.bean;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import me.qyh.blog.message.Message;
import me.qyh.blog.message.MessageListSerializer;

public class ImportSuccess {

	private int index;
	@JsonSerialize(using = MessageListSerializer.class)
	private List<Message> warnings = new ArrayList<Message>();

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	public List<Message> getWarnings() {
		return warnings;
	}

	public void setWarning(List<Message> warnings) {
		this.warnings = warnings;
	}

	public ImportSuccess() {

	}

	public ImportSuccess(int index) {
		this.index = index;
	}

	public void addWarning(Message warning) {
		this.warnings.add(warning);
	}

}
