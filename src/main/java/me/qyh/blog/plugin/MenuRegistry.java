package me.qyh.blog.plugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MenuRegistry {

	private static final MenuRegistry REGISTRY = new MenuRegistry();

	private List<Menu> menus = new ArrayList<>();

	private MenuRegistry() {
		super();
	}

	public MenuRegistry addMenu(Menu menu) {
		menus.add(menu);
		return this;
	}

	public List<Menu> getMenus() {
		return Collections.unmodifiableList(menus);
	}

	public static MenuRegistry getInstance() {
		return REGISTRY;
	}

}
