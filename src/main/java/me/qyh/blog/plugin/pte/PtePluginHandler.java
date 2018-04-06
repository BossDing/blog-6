package me.qyh.blog.plugin.pte;

import org.springframework.context.ApplicationContext;
import org.springframework.web.context.WebApplicationContext;

import me.qyh.blog.plugin.PluginHandlerAdapter;
import me.qyh.blog.template.service.TemplateService;

public class PtePluginHandler extends PluginHandlerAdapter {

	@Override
	public void init(ApplicationContext applicationContext) {
		TemplateService templateService = applicationContext.getBean(TemplateService.class);
		((WebApplicationContext) applicationContext).getServletContext()
				.addListener(new PreviewTemplateEvitSessionListener(templateService));
	}

}
