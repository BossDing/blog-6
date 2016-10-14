package me.qyh.blog.file;

import me.qyh.blog.entity.Id;

public class CommonFile extends Id {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private long size;// 文件大小，该大小仅仅是本服务上的文件大小，并不代表其他存储服务上的文件大小
	private String extension;// 后缀名
	private int store;
	private int server;
	private String originalFilename;// 原始文件名

	private Integer width;// 图片等文件
	private Integer height;// 图片等文件

	public CommonFile() {
	}

	public long getSize() {
		return size;
	}

	public void setSize(long size) {
		this.size = size;
	}

	public String getExtension() {
		return extension;
	}

	public void setExtension(String extension) {
		this.extension = extension;
	}

	public String getOriginalFilename() {
		return originalFilename;
	}

	public void setOriginalFilename(String originalFilename) {
		this.originalFilename = originalFilename;
	}

	public Integer getWidth() {
		return width;
	}

	public void setWidth(Integer width) {
		this.width = width;
	}

	public Integer getHeight() {
		return height;
	}

	public void setHeight(Integer height) {
		this.height = height;
	}

	public boolean isImage() {
		return ImageHelper.isImage(extension);
	}

	public int getStore() {
		return store;
	}

	public void setStore(int store) {
		this.store = store;
	}

	public int getServer() {
		return server;
	}

	public void setServer(int server) {
		this.server = server;
	}
}
