package me.qyh.blog.input;

import java.util.ArrayList;
import java.util.List;

import me.qyh.util.Validators;

public class Tags {

	private String simpleTags;
	private List<Tag> tags = new ArrayList<Tag>();

	public void setSimpleTags(String simpleTags) {
		this.simpleTags = simpleTags;
	}

	public List<Tag> getTags() {
		if (!Validators.isEmptyOrNull(simpleTags, true)) {
			String[] names = this.simpleTags.split(",");
			for (String name : names) {
				if (name.isEmpty()) {
					continue;
				}
				Tag tag = new Tag();
				tag.setName(name);
				tags.add(tag);
			}
		}
		return tags;
	}

	public void setTags(List<Tag> tags) {
		this.tags = tags;
	}

	public void addTag(Tag tag) {
		this.tags.add(tag);
	}

}
