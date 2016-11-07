package me.qyh.blog.file;

public class ThumbnailUrl {

	private String small;
	private String middle;
	private String large;

	public ThumbnailUrl(String small, String middle, String large) {
		super();
		this.small = small;
		this.middle = middle;
		this.large = large;
	}

	public String getSmall() {
		return small;
	}

	public String getMiddle() {
		return middle;
	}

	public String getLarge() {
		return large;
	}

}
