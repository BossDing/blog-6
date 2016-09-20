package me.qyh.blog.ui.widget;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;

import me.qyh.blog.entity.Id;
import me.qyh.blog.ui.page.Page;

public class WidgetTpl extends Id implements Comparable<WidgetTpl> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@JsonIgnore
	private Page page;
	private String tpl;
	private Widget widget;
	@JsonIgnore
	private int order;

	public Page getPage() {
		return page;
	}

	public void setPage(Page page) {
		this.page = page;
	}

	public String getTpl() {
		return tpl;
	}

	public void setTpl(String tpl) {
		this.tpl = tpl;
	}

	public Widget getWidget() {
		return widget;
	}

	public void setWidget(Widget widget) {
		this.widget = widget;
	}

	@JsonIgnore
	public Map<String, Object> getTemplateDatas() {
		return widget.getTemplateDatas();
	}

	public WidgetTpl() {

	}

	public WidgetTpl(WidgetTpl tpl) {
		setId(tpl.getId());
		this.tpl = tpl.tpl;
		this.page = tpl.page;
		this.widget = tpl.widget;
		this.order = tpl.order;
	}

	public int getOrder() {
		return order;
	}

	public void setOrder(int order) {
		this.order = order;
	}

	@Override
	public int compareTo(WidgetTpl o) {
		return order > o.order ? 1 : (order == o.order) ? 0 : -1;
	}
}
