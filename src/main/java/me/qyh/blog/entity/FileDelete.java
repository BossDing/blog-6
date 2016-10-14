package me.qyh.blog.entity;

import me.qyh.blog.entity.BlogFile.BlogFileType;

public class FileDelete extends Id {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String key;
	private BlogFileType type;
	private Integer store;
	private Integer server;

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public BlogFileType getType() {
		return type;
	}

	public void setType(BlogFileType type) {
		this.type = type;
	}

	public Integer getStore() {
		return store;
	}

	public void setStore(Integer store) {
		this.store = store;
	}

	public Integer getServer() {
		return server;
	}

	public void setServer(Integer server) {
		this.server = server;
	}

}
