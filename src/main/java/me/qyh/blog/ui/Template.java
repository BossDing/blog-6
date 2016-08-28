package me.qyh.blog.ui;

import java.util.Map;

import org.thymeleaf.exceptions.TemplateProcessingException;

public interface Template {

	/**
	 * 获取模板名
	 * 
	 * @return
	 */
	public String getTemplateName();

	/**
	 * 获取模板
	 * 
	 * @return
	 */
	public String getTpl();

	/**
	 * 获取关联模版
	 * 
	 * @param templateName
	 * @return
	 */
	public Template find(String templateName) throws TemplateProcessingException;

	/**
	 * 获取模板解析数据
	 * 
	 * @return
	 */
	public Map<String, Object> getTemplateDatas();
}
