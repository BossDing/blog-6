package me.qyh.blog.api.sitemap;

import java.sql.Timestamp;

import com.google.common.html.HtmlEscapers;

import me.qyh.blog.ui.utils.Times;

public class SiteMapUrlItem {

	private final String loc;
	private final Timestamp lastmod;
	private final Changefreq changefreq;
	private final String priority;

	public SiteMapUrlItem(String loc, Timestamp lastmod, Changefreq changefreq, String priority) {
		super();
		this.loc = loc;
		this.lastmod = lastmod;
		this.changefreq = changefreq;
		this.priority = priority;
	}

	public StringBuilder toBuilder() {
		StringBuilder sb = new StringBuilder();
		sb.append("<url>");
		sb.append("<loc>").append(cleanUrl(loc)).append("</loc>");
		if (lastmod != null) {
			sb.append("<lastmod>").append(Times.format(lastmod.toLocalDateTime(), "yyyy-MM-dd")).append("</lastmod>");
		}
		if (changefreq != null) {
			sb.append("<changefreq>").append(changefreq.name().toLowerCase()).append("</changefreq>");
		}
		if (priority != null) {
			sb.append("<priority>").append(priority).append("</priority>");
		}
		sb.append("</url>");
		return sb;
	}

	private String cleanUrl(String url) {
		return HtmlEscapers.htmlEscaper().escape(url);
	}
}