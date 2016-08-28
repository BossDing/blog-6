package me.qyh.blog.bean;

import me.qyh.blog.file.CommonFile;

public class ExpandedCommonFile extends CommonFile {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String previewUrl;
	private String url;
	private String downloadUrl;

	public String getPreviewUrl() {
		return previewUrl;
	}

	public void setPreviewUrl(String previewUrl) {
		this.previewUrl = previewUrl;
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
