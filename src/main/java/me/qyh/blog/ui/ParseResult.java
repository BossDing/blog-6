package me.qyh.blog.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import me.qyh.blog.ui.data.DataBind;
import me.qyh.blog.ui.fragment.Fragment;

public class ParseResult {

	private List<DataBind<?>> binds = new ArrayList<DataBind<?>>();
	private Map<String, Fragment> fragments = new HashMap<String, Fragment>();
	private Set<DataTag> unkownDatas = new LinkedHashSet<DataTag>();
	private Set<String> unkownFragments = new LinkedHashSet<String>();

	public List<DataBind<?>> getBinds() {
		return binds;
	}

	public void setBinds(List<DataBind<?>> binds) {
		this.binds = binds;
	}

	public Set<DataTag> getUnkownDatas() {
		return unkownDatas;
	}

	public Set<String> getUnkownFragments() {
		return unkownFragments;
	}

	public void addUnkownData(DataTag tag) {
		unkownDatas.add(tag);
	}

	public void addUnkownFragment(String name) {
		unkownFragments.add(name);
	}

	public Map<String, Fragment> getFragments() {
		return fragments;
	}

	public void putFragment(String key, Fragment v) {
		fragments.put(key, v);
	}

}
