package me.qyh.blog.ui;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import me.qyh.blog.ui.widget.WidgetTpl;

public class ParseResult {

	private String pageTpl;
	private List<WidgetTpl> tpls;
	private Set<String> unkownWidgets = new HashSet<String>();

	public ParseResult(String pageTpl, List<WidgetTpl> tpls, Set<String> unkownWidgets) {
		super();
		this.pageTpl = pageTpl;
		this.tpls = tpls;
		this.unkownWidgets = unkownWidgets;
	}

	public String getPageTpl() {
		return pageTpl;
	}

	public List<WidgetTpl> getTpls() {
		return tpls;
	}

	public Set<String> getUnkownWidgets() {
		return unkownWidgets;
	}

}
