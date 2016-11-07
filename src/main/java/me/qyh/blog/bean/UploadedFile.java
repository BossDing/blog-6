package me.qyh.blog.bean;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import me.qyh.blog.file.ThumbnailUrl;
import me.qyh.blog.message.Message;
import me.qyh.blog.message.MessageSerializer;

/**
 * 文件上传结果
 * 
 * @author mhlx
 *
 */
public class UploadedFile {
	@JsonSerialize(using = MessageSerializer.class)
	private Message error;// 上传失败原因;
	private long size;// 上传文件大小
	private String name;// 上传文件名称
	private ThumbnailUrl thumbnailUrl;// 缩略图路径
	private String url;// 访问路径

	public UploadedFile(String name, Message error) {
		this.error = error;
		this.name = name;
	}

	public UploadedFile(String name, long size, ThumbnailUrl thumbnailUrl, String url) {
		this.url = url;
		this.size = size;
		this.name = name;
		this.thumbnailUrl = thumbnailUrl;
	}

	public Message getError() {
		return error;
	}

	public long getSize() {
		return size;
	}

	public String getName() {
		return name;
	}

	public boolean hasError() {
		return error != null;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public ThumbnailUrl getThumbnailUrl() {
		return thumbnailUrl;
	}

}
