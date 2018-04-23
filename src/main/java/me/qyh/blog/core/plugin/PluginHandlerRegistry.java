/*
 * Copyright 2016 qyh.me
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package me.qyh.blog.core.plugin;

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

import me.qyh.blog.core.exception.SystemException;
import me.qyh.blog.core.util.Validators;

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
	@Autowired
	private ArticleContentHandlerRegistry articleContentHandlerRegistry;
	@Autowired
	private FileStoreRegistry fileStoreRegistry;
	@Autowired
	private TemplateInterceptorRegistry templateInterceptorRegistry;
	@Autowired
	private HandlerInterceptorRegistry handlerInterceptorRegistry;
	@Autowired
	private LockProviderRegistry lockProviderRegistry;
	@Autowired
	private SuccessfulLoginHandlerRegistry successfulLoginHandlerRegistry;
	@Autowired
	private LogoutHandlerRegistry logoutHandlerRegistry;
	@Autowired
	private ArticleHitHandlerRegistry articleHitHandlerRegistry;

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

	private void invokePluginHandler(PluginHandler pluginHandler, ApplicationContext applicationContext)
			throws Exception {
		SpringBeanAutowiringSupport.processInjectionBasedOnCurrentContext(pluginHandler);

		pluginHandler.init(applicationContext);
		pluginHandler.addDataTagProcessor(dataTagProcessorRegistry);
		pluginHandler.addTemplate(templateRegistry);
		pluginHandler.addRequestHandlerMapping(requestMappingRegistry);
		pluginHandler.addExceptionHandler(exceptionHandlerRegistry);
		pluginHandler.addArticleContentHandler(articleContentHandlerRegistry);
		pluginHandler.addMenu(MenuRegistry.getInstance());
		pluginHandler.addFileStore(fileStoreRegistry);
		pluginHandler.addTemplateInterceptor(templateInterceptorRegistry);
		pluginHandler.addHandlerInterceptor(handlerInterceptorRegistry);
		pluginHandler.addLockProvider(lockProviderRegistry);
		pluginHandler.addSuccessfulLoginHandler(successfulLoginHandlerRegistry);
		pluginHandler.addLogoutHandler(logoutHandlerRegistry);
		pluginHandler.addHitHandler(articleHitHandlerRegistry);
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
			resources = null;
		}

		if (!Validators.isEmpty(resources)) {

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
						newInstance = (PluginHandler) handlerClass.getConstructor().newInstance();
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
