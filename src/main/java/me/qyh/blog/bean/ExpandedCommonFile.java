package me.qyh.blog.bean;

import me.qyh.blog.file.CommonFile;
import me.qyh.blog.file.ThumbnailUrl;

public class ExpandedCommonFile extends CommonFile {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private ThumbnailUrl thumbnailUrl;
	private String url;
	private String downloadUrl;

	public ThumbnailUrl getThumbnailUrl() {
		return thumbnailUrl;
	}

	public void setThumbnailUrl(ThumbnailUrl thumbnailUrl) {
		this.thumbnailUrl = thumbnailUrl;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getDownloadUrl() {
		return downloadUrl;
	}

	public void setDownloadUrl(String downloadUrl) {
		this.downloadUrl = downloadUrl;
	}

}
