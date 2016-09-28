package me.qyh.blog.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import me.qyh.blog.ui.data.DataBind;
import me.qyh.blog.ui.fragement.Fragement;

public class ParseResult {

	private List<DataBind<?>> binds = new ArrayList<DataBind<?>>();
	private Map<String, Fragement> fragements = new HashMap<String, Fragement>();
	private Set<DataTag> unkownDatas = new LinkedHashSet<DataTag>();
	private Set<String> unkownFragements = new LinkedHashSet<String>();

	public List<DataBind<?>> getBinds() {
		return binds;
	}

	public void setBinds(List<DataBind<?>> binds) {
		this.binds = binds;
	}

	public Set<DataTag> getUnkownDatas() {
		return unkownDatas;
	}

	public Set<String> getUnkownFragements() {
		return unkownFragements;
	}

	public void addUnkownData(DataTag tag) {
		unkownDatas.add(tag);
	}

	public void addUnkownFragement(String name) {
		unkownFragements.add(name);
	}

	public Map<String, Fragement> getFragements() {
		return fragements;
	}

	public void putFragement(String key, Fragement v) {
		fragements.put(key, v);
	}

}
