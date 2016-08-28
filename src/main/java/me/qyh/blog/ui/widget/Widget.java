package me.qyh.blog.ui.widget;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import me.qyh.blog.entity.Id;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Widget extends Id {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * 系统挂件和用户自定义挂件不允许挂件名重复
	 */
	private String name;// 模板名
	private WidgetType type;
	@JsonProperty
	private String defaultTpl;// 默认模板

	public enum WidgetType {
		SYSTEM, USER;
	}

	public WidgetType getType() {
		return type;
	}

	public void setType(WidgetType type) {
		this.type = type;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@JsonIgnore
	public Map<String, Object> getTemplateDatas() {
		return null;
	}

	@JsonIgnore
	public String getDefaultTpl() {
		return defaultTpl;
	}

	public void setDefaultTpl(String defaultTpl) {
		this.defaultTpl = defaultTpl;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!Widget.class.isAssignableFrom(obj.getClass())) {
			return false;
		}
		Widget other = (Widget) obj;
		if (name == null || other.name == null) {
			return false;
		}
		if (name == null) {
			if (other.name != null) {
				return false;
			}
		} else if (!name.equals(other.name)) {
			return false;
		}
		return true;
	}
}
