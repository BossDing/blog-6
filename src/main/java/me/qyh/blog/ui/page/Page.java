package me.qyh.blog.ui.page;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import me.qyh.blog.entity.Id;
import me.qyh.blog.entity.Space;
import me.qyh.blog.exception.SystemException;
import me.qyh.blog.ui.widget.WidgetTpl;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Page extends Id implements Cloneable {

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

	@JsonIgnore
	public String getTemplateName() {
		return PREFIX + getId() + "-" + getType();
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

	public static boolean isTpl(String templateName) {
		return templateName.startsWith(PREFIX);
	}

	public WidgetTpl getWidgetTpl(String name) {
		for (WidgetTpl widgetTpl : tpls) {
			if (name.equals(widgetTpl.getWidget().getName())) {
				return widgetTpl;
			}
		}
		return null;
	}

	@JsonIgnore
	public Map<String, Object> getTemplateDatas() {
		Map<String, Object> datas = new HashMap<String, Object>();
		for (WidgetTpl widget : tpls) {
			Map<String, Object> _datas = widget.getTemplateDatas();
			if (!CollectionUtils.isEmpty(_datas)) {
				datas.putAll(_datas);
			}
		}
		return datas;
	}

	@Override
	public Object clone() {
		try {
			return super.clone();
		} catch (CloneNotSupportedException e) {
			throw new SystemException(e.getMessage(), e);
		}
	}

}
