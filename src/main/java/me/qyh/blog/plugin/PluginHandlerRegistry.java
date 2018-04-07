package me.qyh.blog.plugin;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
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
import org.thymeleaf.util.ArrayUtils;

import me.qyh.blog.core.exception.SystemException;

public class PluginHandlerRegistry
		implements ResourceLoaderAware, ApplicationContextInitializer<ConfigurableApplicationContext> {

	private static final Logger logger = LoggerFactory.getLogger(PluginHandlerRegistry.class);

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

	private Set<String> plugins = new HashSet<>();
	private static final List<PluginHandler> handlerInstances = new ArrayList<>();

	@EventListener
	@Order(value = Ordered.LOWEST_PRECEDENCE)
	void start(ContextRefreshedEvent evt) throws Exception {
		if (evt.getApplicationContext().getParent() == null) {
			return;
		}
		if (!handlerInstances.isEmpty()) {
			ApplicationContext applicationContext = evt.getApplicationContext();
			CountDownLatch cdl = new CountDownLatch(1);

			new Thread(() -> {

				try {
					for (PluginHandler pluginHandler : handlerInstances) {
						try {
							invokePluginHandler(pluginHandler, applicationContext);
							plugins.add(getPluginName(pluginHandler.getClass()));
						} catch (Exception e) {
							logger.warn("加载插件：" + PluginHandler.class.getName() + "失败", e);
						}
					}
				} finally {
					cdl.countDown();
				}

			}).start();

			cdl.await();

			handlerInstances.clear();
		}
	}

	public Set<String> getPlugins() {
		return Collections.unmodifiableSet(plugins);
	}

	private String getPluginName(Class<? extends PluginHandler> clazz) {
		String fullName = clazz.getClass().getPackage().getName();
		return fullName.substring(fullName.lastIndexOf('.') + 1, fullName.length());
	}

	private void invokePluginHandler(PluginHandler pluginHandler, ApplicationContext applicationContext) {
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
	}

	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	@Override
	public void initialize(ConfigurableApplicationContext applicationContext) {
		// create new PluginHandler instance

		ResourcePatternResolver resolver = ResourcePatternUtils.getResourcePatternResolver(resourceLoader);
		MetadataReaderFactory metadataReaderFactory = new SimpleMetadataReaderFactory(resolver);
		Resource[] resources;
		try {
			resources = resolver.getResources("classpath:me/qyh/blog/plugin/*/*PluginHandler.class");
		} catch (IOException e) {
			throw new SystemException(e.getMessage(), e);
		}

		if (!ArrayUtils.isEmpty(resources)) {

			for (Resource res : resources) {
				Class<?> handlerClass;
				try {
					MetadataReader reader = metadataReaderFactory.getMetadataReader(res);
					handlerClass = Class.forName(reader.getClassMetadata().getClassName());
				} catch (ClassNotFoundException | IOException e) {
					throw new SystemException(e.getMessage(), e);
				}
				if (PluginHandler.class.isAssignableFrom(handlerClass)) {
					PluginHandler newInstance;
					try {
						newInstance = (PluginHandler) handlerClass.newInstance();
						newInstance.initialize(applicationContext);
						handlerInstances.add(newInstance);
					} catch (Exception e) {
						logger.warn("加载插件：" + PluginHandler.class.getName() + "失败", e);
					}
				}
			}

		}
	}

}
