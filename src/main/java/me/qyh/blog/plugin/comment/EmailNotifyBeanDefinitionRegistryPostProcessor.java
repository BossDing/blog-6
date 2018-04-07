package me.qyh.blog.plugin.comment;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;

public class EmailNotifyBeanDefinitionRegistryPostProcessor implements BeanDefinitionRegistryPostProcessor {

	private final EmailNofityConfig config;

	EmailNotifyBeanDefinitionRegistryPostProcessor(EmailNofityConfig config) {
		super();
		this.config = config;
	}

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		//
	}

	@Override
	public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
		BeanDefinition definition = BeanDefinitionBuilder.genericBeanDefinition(CommentEmailNotify.class)
				.setScope(BeanDefinition.SCOPE_SINGLETON).addConstructorArgValue(config).getBeanDefinition();
		registry.registerBeanDefinition(CommentEmailNotify.class.getName(), definition);
	}

}
