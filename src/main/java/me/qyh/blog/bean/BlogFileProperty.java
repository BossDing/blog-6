package me.qyh.blog.bean;

import java.sql.Timestamp;
import java.util.List;

import me.qyh.blog.entity.BlogFile.BlogFileType;

public class BlogFileProperty {

	private long totalSize;// 总大小
	private Timestamp lastModifyDate;// 最后修改日期
	private List<BlogFileCount> counts;
	private BlogFileType type;
	private String downloadUrl;
	private String url;

	public long getTotalSize() {
		return totalSize;
	}

	public void setTotalSize(long totalSize) {
		this.totalSize = totalSize;
	}

	public Timestamp getLastModifyDate() {
		return lastModifyDate;
	}

	public void setLastModifyDate(Timestamp lastModifyDate) {
		this.lastModifyDate = lastModifyDate;
	}

	public List<BlogFileCount> getCounts() {
		return counts;
	}

	public void setCounts(List<BlogFileCount> counts) {
		this.counts = counts;
	}

	public BlogFileType getType() {
		return type;
	}

	public void setType(BlogFileType type) {
		this.type = type;
	}

	public String getDownloadUrl() {
		return downloadUrl;
	}

	public void setDownloadUrl(String downloadUrl) {
		this.downloadUrl = downloadUrl;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

}
