package me.qyh.blog.metaweblog;

import me.qyh.blog.message.Message;

public final class FaultException extends Exception {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String code;
	private Message desc;

	public FaultException(String code, Message dest) {
		super();
		this.code = code;
		this.desc = dest;
	}

	public String getCode() {
		return code;
	}

	public Message getDesc() {
		return desc;
	}

}