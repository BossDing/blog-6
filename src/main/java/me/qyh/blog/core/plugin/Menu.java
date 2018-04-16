package me.qyh.blog.core.plugin;

import java.util.ArrayList;
import java.util.List;

import me.qyh.blog.core.util.StringUtils;

public class Menu {

	private final String name;
	private final String path;
	private final String id;

	private List<Menu> children = new ArrayList<>();

	public Menu(String name, String path) {
		super();
		this.name = name;
		this.path = path;
		this.id = StringUtils.uuid();
	}

	public Menu(String name) {
		this(name, null);
	}

	public String getName() {
		return name;
	}

	public String getPath() {
		return path;
	}

	public List<Menu> getChildren() {
		return children;
	}

	public void setChildren(List<Menu> children) {
		this.children = children;
	}

	public Menu addChild(Menu child) {
		children.add(child);
		return this;
	}

	public String getId() {
		return id;
	}

}