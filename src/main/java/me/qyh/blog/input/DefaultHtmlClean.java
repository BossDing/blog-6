package me.qyh.blog.input;

import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;

import me.qyh.blog.config.Constants;
import me.qyh.util.Jsons;
import me.qyh.util.Validators;

public class DefaultHtmlClean implements HtmlClean, InitializingBean {

	/**
	 * whitelist的json配置,请小心配置注意xss，当且仅当配置评论允许html之后才会生效;
	 * 
	 * @see JsonWhitelist
	 */
	private Resource whitelistJsonResource;
	private Tags tags;

	private static final String DEFAULT_WHITE_LIST_JSON = "{\"simpleTags\":\"b,code,em,del,small,strong\"}";

	@Override
	public String clean(String html) {
		return Jsoup.clean(html, _Whitelist.configured(tags));
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (tags == null) {
			if (whitelistJsonResource != null) {
				InputStream is = null;
				try {
					is = whitelistJsonResource.getInputStream();
					tags = Jsons.readValue(Tags.class, IOUtils.toString(is, Constants.CHARSET));
				} finally {
					IOUtils.closeQuietly(is);
				}
			} else {
				tags = Jsons.readValue(Tags.class, DEFAULT_WHITE_LIST_JSON);
			}
		}
	}

	private static final class _Whitelist extends Whitelist {
		_Whitelist(Tags tags) {
			for (Tag tag : tags.getTags()) {
				addTags(tag.getName());
				for (Attribute att : tag.getAttributes()) {
					addAttributes(tag.getName(), att.getName());
					if (!Validators.isEmptyOrNull(att.getProtocols(), true)) {
						String protocols = att.getProtocols().trim();
						for (String protocol : protocols.split(",")) {
							addProtocols(tag.getName(), att.getName(), protocol);
						}
					}
					if (!Validators.isEmptyOrNull(att.getEnforce(), true)) {
						String enforce = att.getEnforce().trim();
						addEnforcedAttribute(tag.getName(), att.getName(), enforce);
					}
				}
			}
		}

		static Whitelist configured(Tags tags) {
			return new _Whitelist(tags);
		}
	}

	public void setTags(Tags tags) {
		this.tags = tags;
	}

	public void setWhitelistJsonResource(Resource whitelistJsonResource) {
		this.whitelistJsonResource = whitelistJsonResource;
	}

}
