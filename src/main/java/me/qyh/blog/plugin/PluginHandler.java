package me.qyh.blog.plugin;

import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.RequestMapping;

import me.qyh.blog.template.render.data.DataTagProcessor;

/**
 * @author wwwqyhme
 *
 */
public interface PluginHandler {

	/**
	 * 初始化，会被最先调用
	 * 
	 * @param applicationContext
	 */
	void init(ApplicationContext applicationContext);

	/**
	 * 添加DataTagProcessor
	 * 
	 * @see DataTagProcessor
	 * @param registry
	 */
	void addDataTagProcessor(DataTagProcessorRegistry registry);

	/**
	 * 添加模板
	 * 
	 * @param registry
	 */
	void addTemplate(TemplateRegistry registry);

	/**
	 * 添加RequestMapping
	 * 
	 * @see RequestMapping
	 * @param registry
	 */
	void addRequestHandlerMapping(RequestMappingRegistry registry);

	/**
	 * 添加管理台餐单
	 * 
	 * @param registry
	 */
	void addMenu(MenuRegistry registry);
}
