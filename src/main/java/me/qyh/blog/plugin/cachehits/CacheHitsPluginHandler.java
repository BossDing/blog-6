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
package me.qyh.blog.plugin.cachehits;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.ConfigurableApplicationContext;

import me.qyh.blog.core.plugin.PluginHandler;
import me.qyh.blog.core.plugin.PluginProperties;

public class CacheHitsPluginHandler implements PluginHandler {

	private final PluginProperties pluginProperties = PluginProperties.getInstance();
	private final boolean enable = pluginProperties.get("plugin.cachehits.enable").map(Boolean::parseBoolean)
			.orElse(false);

	private static final String VALIDIP_KEY = "plugin.cachehits.validip";
	private static final String MAXIPS_KEY = "plugin.cachehits.maxIps";
	private static final String FLUSHNUM_KEY = "plugin.cachehits.flushNum";
	private static final String FLUSHSEC_KEY = "plugin.cachehits.flushSec";

	@Override
	public void initialize(ConfigurableApplicationContext applicationContext) {
		if (enable) {
			applicationContext.addBeanFactoryPostProcessor(new BeanDefinitionRegistryPostProcessor() {

				@Override
				public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {

				}

				@Override
				public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
					boolean validIp = pluginProperties.get(VALIDIP_KEY).map(Boolean::parseBoolean).orElse(false);
					int maxIps = pluginProperties.get(MAXIPS_KEY).map(Integer::parseInt).orElse(100);
					int flushNum = pluginProperties.get(FLUSHNUM_KEY).map(Integer::parseInt).orElse(50);
					int flushSec = pluginProperties.get(FLUSHSEC_KEY).map(Integer::parseInt).orElse(600);

					BeanDefinition definition = BeanDefinitionBuilder.genericBeanDefinition(CacheableHitsStrategy.class)
							.setScope(BeanDefinition.SCOPE_SINGLETON).addConstructorArgValue(validIp)
							.addConstructorArgValue(maxIps).addConstructorArgValue(flushNum)
							.addConstructorArgValue(flushSec).getBeanDefinition();
					registry.registerBeanDefinition(CacheableHitsStrategy.class.getName(), definition);
				}
			});
		}
	}

}
