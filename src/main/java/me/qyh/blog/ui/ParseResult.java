package me.qyh.blog.ui;

import java.util.List;

import me.qyh.blog.ui.widget.WidgetTpl;

public class ParseResult {

	private String pageTpl;
	private List<WidgetTpl> tpls;

	public ParseResult(String pageTpl, List<WidgetTpl> tpls) {
		super();
		this.pageTpl = pageTpl;
		this.tpls = tpls;
	}

	public String getPageTpl() {
		return pageTpl;
	}

	public List<WidgetTpl> getTpls() {
		return tpls;
	}

}
