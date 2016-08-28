package me.qyh.blog.web.controller.form;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

public class BlogFileUpload {

	private List<MultipartFile> files;
	private Integer parent;
	private Integer server;

	public List<MultipartFile> getFiles() {
		return files;
	}

	public void setFiles(List<MultipartFile> files) {
		this.files = files;
	}

	public Integer getParent() {
		return parent;
	}

	public void setParent(Integer parent) {
		this.parent = parent;
	}

	public Integer getServer() {
		return server;
	}

	public void setServer(Integer server) {
		this.server = server;
	}

	@Override
	public String toString() {
		return "BlogFileUpload [files=" + files + ", parent=" + parent + ", server=" + server + "]";
	}

}
