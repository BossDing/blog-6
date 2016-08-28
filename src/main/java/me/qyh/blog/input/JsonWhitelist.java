package me.qyh.blog.input;

import java.io.IOException;

import org.jsoup.safety.Whitelist;

import com.fasterxml.jackson.core.JsonProcessingException;

import me.qyh.util.Jsons;
import me.qyh.util.Validators;

/**
 * @see <a href="whitelist.json">whitelist_basic.json</a>
 * @author Administrator
 *
 */
public class JsonWhitelist extends Whitelist {

	public static Whitelist json(String json) throws JsonProcessingException, IOException {
		return new JsonWhitelist(json);
	}

	private JsonWhitelist(String json) throws JsonProcessingException, IOException {
		Tags tags = Jsons.readValue(Tags.class, json);
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
}
