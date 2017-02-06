/*
 * Copyright 2016 qyh.me
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package me.qyh.blog.api.sitemap;

import java.sql.Timestamp;

import com.google.common.html.HtmlEscapers;

import me.qyh.blog.util.Times;

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