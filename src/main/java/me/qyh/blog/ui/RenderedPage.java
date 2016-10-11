package me.qyh.blog.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import me.qyh.blog.ui.data.DataBind;
import me.qyh.blog.ui.fragment.Fragment;
import me.qyh.blog.ui.page.Page;

public class RenderedPage {
	private Page page;
	private List<DataBind<?>> binds = new ArrayList<>();
	private Map<String, Fragment> fragmentMap = new LinkedHashMap<>();

	public RenderedPage(Page page, List<DataBind<?>> binds, Map<String, Fragment> fragmentMap) {
		this.page = page;
		this.binds = binds;
		this.fragmentMap = fragmentMap;
	}

	public Map<String, Object> getDatas() {
		Map<String, Object> map = new HashMap<>();
		for (DataBind<?> bind : binds) {
			map.put(bind.getDataName(), bind.getData());
		}
		return map;
	}

	public Page getPage() {
		return page;
	}

	public void setPage(Page page) {
		this.page = page;
	}

	public Map<String, Fragment> getFragmentMap() {
		return fragmentMap;
	}

	public String getTemplateName() {
		return page.getTemplateName();
	}
}
