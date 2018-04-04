package me.qyh.blog.plugin;

import org.springframework.context.ApplicationContext;

public interface PluginHandler {

	void init(ApplicationContext applicationContext);

	void addDataTagProcessor(DataTagProcessorRegistry registry);

	void addTemplate(TemplateRegistry registry);

	void addRequestHandlerMapping(RequestMappingRegistry registry);

	void addMenu(MenuRegistry registry);
}
