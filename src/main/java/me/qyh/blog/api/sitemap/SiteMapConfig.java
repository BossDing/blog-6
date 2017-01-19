package me.qyh.blog.api.sitemap;

import java.text.DecimalFormat;

import me.qyh.blog.exception.SystemException;

public class SiteMapConfig {

	private final Changefreq freq;
	private final float priority;

	public SiteMapConfig(Changefreq freq, float priority) {
		super();
		this.freq = freq;
		this.priority = priority;
	}

	public Changefreq getFreq() {
		return freq;
	}

	public String getFormattedPriority() {
		if (priority < 0 || priority > 1) {
			throw new SystemException("sitemap的priority必须在[0,1]之间，且只有一位小数");
		}
		return new DecimalFormat("#0.0").format(priority);
	}

}
