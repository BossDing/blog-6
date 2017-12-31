package me.qyh.blog.template.entity;

import java.sql.Timestamp;

import me.qyh.blog.core.entity.BaseEntity;
import me.qyh.blog.template.Template;

/**
 * @since 2017/12/27
 * @author wwwqyhme
 *
 */
public class HistoryTemplate extends BaseEntity {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private String templateName;
	private String tpl;// 模板
	private Timestamp time;
	private String remark;

	public HistoryTemplate() {
		super();
	}

	public HistoryTemplate(Template template) {
		this.templateName = template.getTemplateName();
		this.tpl = template.getTemplate();
	}

	public String getTemplateName() {
		return templateName;
	}

	public void setTemplateName(String templateName) {
		this.templateName = templateName;
	}

	public String getTpl() {
		return tpl;
	}

	public void setTpl(String tpl) {
		this.tpl = tpl;
	}

	public Timestamp getTime() {
		return time;
	}

	public void setTime(Timestamp time) {
		this.time = time;
	}

	public String getRemark() {
		return remark;
	}

	public void setRemark(String remark) {
		this.remark = remark;
	}
}
