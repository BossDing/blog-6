package me.qyh.blog.input;

import java.util.ArrayList;
import java.util.List;

public class Tag {

	private String name;
	private List<Attribute> attributes = new ArrayList<Attribute>();

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<Attribute> getAttributes() {
		return attributes;
	}

	public void setAttributes(List<Attribute> attributes) {
		this.attributes = attributes;
	}

	public void addAttribute(Attribute att) {
		this.attributes.add(att);
	}

}
