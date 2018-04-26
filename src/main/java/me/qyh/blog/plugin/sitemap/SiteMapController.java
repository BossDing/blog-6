package me.qyh.blog.plugin.sitemap;

import org.springframework.web.bind.annotation.ResponseBody;

import me.qyh.blog.plugin.sitemap.web.component.SiteMapSupport;

public class SiteMapController {

	private final SiteMapSupport support;

	SiteMapController(SiteMapSupport support) {
		this.support = support;
	}

	@ResponseBody
	public String sitemap() {
		return support.getSiteMapXml();
	}

}
