package me.qyh.blog.plugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.classreading.SimpleMetadataReaderFactory;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;

public class PluginHandlerRegistry implements ResourceLoaderAware {

	@Autowired
	private DataTagProcessorRegistry dataTagProcessorRegistry;
	@Autowired
	private TemplateRegistry templateRegistry;
	@Autowired
	private RequestMappingRegistry requestMappingRegistry;
	@Autowired
	private ExceptionHandlerRegistry exceptionHandlerRegistry;
	@Autowired(required = false)
	private ArticleContentHandlerRegistry articleContentHandlerRegistry;
	@Autowired
	private CommentModuleHandlerRegistry commentModuleHandlerRegistry;
	@Autowired
	private CommentCheckerRegistry commentCheckerRegistry;
	@Autowired
	private FileStoreRegistry fileStoreRegistry;
	@Autowired
	private TemplateInterceptorRegistry templateInterceptorRegistry;
	@Autowired
	private HandlerInterceptorRegistry handlerInterceptorRegistry;

	private ResourceLoader resourceLoader;

	public Set<String> plugins = new HashSet<>();

	@EventListener
	@Order(value = Ordered.LOWEST_PRECEDENCE)
	void start(ContextRefreshedEvent evt) throws Exception {
		if (evt.getApplicationContext().getParent() == null) {
			return;
		}
		ApplicationContext applicationContext = evt.getApplicationContext();
		ResourcePatternResolver resolver = ResourcePatternUtils.getResourcePatternResolver(resourceLoader);
		MetadataReaderFactory metadataReaderFactory = new SimpleMetadataReaderFactory(resolver);
		Resource[] resources = resolver.getResources("classpath:me/qyh/blog/plugin/*/*PluginHandler.class");
		if (resources != null) {

			List<PluginHandler> handlers = new ArrayList<>();

			for (Resource res : resources) {
				MetadataReader reader = metadataReaderFactory.getMetadataReader(res);

				Class<?> handler = Class.forName(reader.getClassMetadata().getClassName());
				if (PluginHandler.class.isAssignableFrom(handler)) {
					handlers.add((PluginHandler) handler.newInstance());
				}
			}

			CountDownLatch cdl = new CountDownLatch(1);

			new Thread(() -> {

				try {
					for (PluginHandler pluginHandler : handlers) {
						SpringBeanAutowiringSupport.processInjectionBasedOnCurrentContext(pluginHandler);

						pluginHandler.init(applicationContext);
						pluginHandler.addDataTagProcessor(dataTagProcessorRegistry);
						pluginHandler.addTemplate(templateRegistry);
						pluginHandler.addRequestHandlerMapping(requestMappingRegistry);
						pluginHandler.addExceptionHandler(exceptionHandlerRegistry);
						if (articleContentHandlerRegistry != null) {
							pluginHandler.addArticleContentHandler(articleContentHandlerRegistry);
						}
						pluginHandler.addCommentModuleHandler(commentModuleHandlerRegistry);
						pluginHandler.addCommentChecker(commentCheckerRegistry);
						pluginHandler.addMenu(MenuRegistry.getInstance());
						pluginHandler.addFileStore(fileStoreRegistry);
						pluginHandler.addTemplateInterceptor(templateInterceptorRegistry);
						pluginHandler.addHandlerInterceptorRegistry(handlerInterceptorRegistry);

						String fullName = pluginHandler.getClass().getPackage().getName();
						String pluginName = fullName.substring(fullName.lastIndexOf('.') + 1, fullName.length());
						plugins.add(pluginName);
					}
				} finally {
					cdl.countDown();
				}

			}).start();

			cdl.await();

		}
	}

	public Set<String> getPlugins() {
		return Collections.unmodifiableSet(plugins);
	}

	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

}
