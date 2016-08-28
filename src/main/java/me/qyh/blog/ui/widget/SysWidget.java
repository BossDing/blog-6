package me.qyh.blog.ui.widget;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * id属性为{@link SysWidgetHandler#getId()}
 * 
 * @author 钱宇豪
 * @date 2016年8月5日 下午1:06:12
 */
public class SysWidget extends Widget implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String dataName;
	@JsonIgnore
	private Object data;// 数据

	public Object getData() {
		return data;
	}

	public String getDataName() {
		return dataName;
	}

	public SysWidget() {

	}

	@Override
	public final WidgetType getType() {
		return WidgetType.SYSTEM;
	}

	public void setDataName(String dataName) {
		this.dataName = dataName;
	}

	public void setData(Object data) {
		this.data = data;
	}

	@Override
	@JsonIgnore
	public Map<String, Object> getTemplateDatas() {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put(dataName, data);
		return map;
	}
}
