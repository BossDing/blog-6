package me.qyh.blog.ui.widget;

import java.util.Map;

import org.thymeleaf.exceptions.TemplateProcessingException;

import com.fasterxml.jackson.annotation.JsonIgnore;

import me.qyh.blog.entity.Id;
import me.qyh.blog.ui.Template;
import me.qyh.blog.ui.page.Page;

public class WidgetTpl extends Id implements Template {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	protected static final String PREFIX = "Widget:";

	@JsonIgnore
	private Page page;
	private String tpl;
	private Widget widget;

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

	@Override
	public String getTemplateName() {
		if (widget.hasId()) {
			return PREFIX + widget.getId() + "-" + widget.getType() + "-" + page.getTemplateName();
		} else {
			return PREFIX + widget.getName() + "-" + page.getTemplateName();
		}
	}

	@Override
	public Template find(String templateName) throws TemplateProcessingException {
		if (templateName.equals(getTemplateName())) {
			return this;
		}
		throw new TemplateProcessingException("挂件" + templateName + "不存在，无法被渲染");
	}

	@Override
	@JsonIgnore
	public Map<String, Object> getTemplateDatas() {
		return widget.getTemplateDatas();
	}
}
