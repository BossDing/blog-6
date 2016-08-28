package me.qyh.blog.ui.page;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.util.CollectionUtils;
import org.thymeleaf.exceptions.TemplateProcessingException;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import me.qyh.blog.entity.Id;
import me.qyh.blog.entity.Space;
import me.qyh.blog.ui.Template;
import me.qyh.blog.ui.widget.WidgetTpl;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Page extends Id implements Template {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	protected static final String PREFIX = "Page:";

	private Space space;// 所属空间
	private String tpl;// 组织片段，用来将挂件组织在一起
	private List<WidgetTpl> tpls = new ArrayList<WidgetTpl>();// 页面关联的挂件
	private PageType type;

	public enum PageType {
		SYSTEM, USER, EXPANDED, ERROR
	}

	public Space getSpace() {
		return space;
	}

	public void setSpace(Space space) {
		this.space = space;
	}

	public List<WidgetTpl> getTpls() {
		return tpls;
	}

	public void setTpls(List<WidgetTpl> tpls) {
		this.tpls = tpls;
	}

	public void setTpl(String tpl) {
		this.tpl = tpl;
	}

	public String getTpl() {
		return tpl;
	}

	public PageType getType() {
		return type;
	}

	public void setType(PageType type) {
		this.type = type;
	}

	@Override
	public String getTemplateName() {
		return PREFIX + getId() + "-" + getType();
	}

	@Override
	public Template find(String templateName) throws TemplateProcessingException {
		if (templateName.startsWith(PREFIX)) {
			if (templateName.equals(getTemplateName())) {
				return this;
			}
			throw new TemplateProcessingException("页面" + templateName + "不存在，无法被渲染");
		}
		if (!CollectionUtils.isEmpty(tpls)) {
			for (WidgetTpl widgetTpl : tpls) {
				if (widgetTpl.getTemplateName().equals(templateName)) {
					return widgetTpl;
				}
			}
		}
		return null;
	}

	@Override
	@JsonIgnore
	public Map<String, Object> getTemplateDatas() {
		Map<String, Object> datas = new HashMap<String, Object>();
		if (!CollectionUtils.isEmpty(tpls)) {
			for (WidgetTpl widget : tpls) {
				Map<String, Object> _datas = widget.getTemplateDatas();
				if (!CollectionUtils.isEmpty(_datas)) {
					datas.putAll(_datas);
				}
			}
		}
		return datas;
	}

	public Page() {
		super();
	}

	public Page(Integer id) {
		super(id);
	}

	public Page(Space space) {
		this.space = space;
	}
}
