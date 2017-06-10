package me.qyh.blog.web.controller.back;

import me.qyh.blog.util.StringUtils;

public final class PreviewUrl {

	private final String url;
	private final boolean hasPathVariable;

	public PreviewUrl(String url) {
		super();
		this.url = url;
		this.hasPathVariable = StringUtils.substringBetween(url, "{", "}") != null;
	}

	public String getUrl() {
		return url;
	}

	public boolean isHasPathVariable() {
		return hasPathVariable;
	}

}
