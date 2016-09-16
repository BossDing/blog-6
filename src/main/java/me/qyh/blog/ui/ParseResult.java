package me.qyh.blog.ui;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import me.qyh.blog.ui.widget.WidgetTpl;

public class ParseResult {

	private List<WidgetTpl> tpls;
	private Set<String> unkownWidgets = new HashSet<String>();

	public ParseResult(List<WidgetTpl> tpls, Set<String> unkownWidgets) {
		super();
		this.tpls = tpls;
		this.unkownWidgets = unkownWidgets;
	}

	public List<WidgetTpl> getTpls() {
		return tpls;
	}

	public Set<String> getUnkownWidgets() {
		return unkownWidgets;
	}

}
