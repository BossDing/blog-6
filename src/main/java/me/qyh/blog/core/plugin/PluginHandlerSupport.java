package me.qyh.blog.core.plugin;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;

import me.qyh.blog.core.util.Validators;

public class PluginHandlerSupport implements PluginHandler {

	@Override
	public final void initialize(ConfigurableApplicationContext applicationContext) {
		initializeOther(applicationContext);
		registerBean(new BeanRegistry(applicationContext));
	}

	@Override
	public final void initializeChild(ConfigurableApplicationContext applicationContext) {
		initializeChildOther(applicationContext);
		registerChildBean(new BeanRegistry(applicationContext));
	}

	protected void registerBean(BeanRegistry registry) {

	}

	protected void registerChildBean(BeanRegistry registry) {

	}

	protected void initializeChildOther(ConfigurableApplicationContext applicationContext) {

	}

	protected void initializeOther(ConfigurableApplicationContext applicationContext) {

	}

	protected BeanDefinition simpleBeanDefinition(Class<?> clazz) {
		return BeanDefinitionBuilder.genericBeanDefinition(clazz).setScope(BeanDefinition.SCOPE_SINGLETON)
				.getBeanDefinition();
	}

	protected class BeanRegistry {
		private final ConfigurableApplicationContext context;

		public BeanRegistry(ConfigurableApplicationContext context) {
			super();
			this.context = context;
		}

		public BeanRegistry scanAndRegister(String... basePackages) {
			if (Validators.isEmpty(basePackages)) {
				return this;
			}
			context.addBeanFactoryPostProcessor(new BeanDefinitionRegistryPostProcessor() {

				@Override
				public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {

				}

				@Override
				public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
					ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(registry);
					scanner.scan(basePackages);
				}
			});
			return this;
		}

		public BeanRegistry register(String beanName, BeanDefinition definition) {
			context.addBeanFactoryPostProcessor(new BeanDefinitionRegistryPostProcessor() {

				@Override
				public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {

				}

				@Override
				public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
					registry.registerBeanDefinition(beanName, definition);
				}
			});
			return this;
		}

	}

}
