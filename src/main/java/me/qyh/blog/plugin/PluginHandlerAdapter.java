package me.qyh.blog.plugin;

import org.springframework.context.ApplicationContext;

public abstract class PluginHandlerAdapter implements PluginHandler {

	@Override
	public void init(ApplicationContext applicationContext) {

	}

	@Override
	public void addDataTagProcessor(DataTagProcessorRegistry registry) {

	}

	@Override
	public void addTemplate(TemplateRegistry registry) {

	}

	@Override
	public void addRequestHandlerMapping(RequestMappingRegistry registry) {

	}

	@Override
	public void addMenu(MenuRegistry registry) {

	}

}
