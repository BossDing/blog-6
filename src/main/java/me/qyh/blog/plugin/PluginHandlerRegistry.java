package me.qyh.blog.plugin;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;

public class PluginHandlerRegistry implements ResourceLoaderAware, ApplicationContextAware {

	@Autowired
	private DataTagProcessorRegistry dataTagProcessorRegistry;
	@Autowired
	private TemplateRegistry templateRegistry;
	@Autowired
	private RequestMappingRegistry requestMappingRegistry;

	private ResourceLoader resourceLoader;

	private ApplicationContext applicationContext;

	@EventListener
	public void start(ContextRefreshedEvent evt) throws Exception {
		ResourcePatternResolver resolver = ResourcePatternUtils.getResourcePatternResolver(resourceLoader);
		MetadataReaderFactory metadataReaderFactory = new CachingMetadataReaderFactory(resolver);
		Resource[] resources = resolver.getResources("classpath:me/qyh/blog/plugin/*/*PluginHandler.class");
		if (resources != null) {

			for (Resource res : resources) {
				MetadataReader reader = metadataReaderFactory.getMetadataReader(res);

				Class<?> handler = Class.forName(reader.getClassMetadata().getClassName());
				if (PluginHandler.class.isAssignableFrom(handler)) {
					Object target = handler.newInstance();
					SpringBeanAutowiringSupport.processInjectionBasedOnCurrentContext(target);

					PluginHandler pluginHandler = (PluginHandler) target;
					pluginHandler.init(applicationContext);
					pluginHandler.addDataTagProcessor(dataTagProcessorRegistry);
					pluginHandler.addTemplate(templateRegistry);
					pluginHandler.addRequestHandlerMapping(requestMappingRegistry);
					pluginHandler.addMenu(MenuRegistry.getInstance());

				}
			}
		}
	}

	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

}
