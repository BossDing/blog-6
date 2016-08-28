package me.qyh.blog.bean;

import me.qyh.blog.file.FileServer;

public class FileServerBean {

	private int id;
	private String name;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public FileServerBean() {

	}

	public FileServerBean(FileServer server) {
		this.id = server.id();
		this.name = server.name();
	}
}
