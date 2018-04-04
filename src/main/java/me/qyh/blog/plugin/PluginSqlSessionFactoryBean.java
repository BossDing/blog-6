package me.qyh.blog.plugin;

import java.io.IOException;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternUtils;

public class PluginSqlSessionFactoryBean extends SqlSessionFactoryBean implements ResourceLoaderAware {

	private ResourceLoader resourceLoader;

	@Override
	protected SqlSessionFactory buildSqlSessionFactory() throws IOException {

		Resource[] resources = ResourcePatternUtils.getResourcePatternResolver(resourceLoader)
				.getResources("classpath:me/qyh/blog/plugin/*/mapper/*.xml");

		if (resources != null) {
			super.setMapperLocations(resources);
		}

		return super.buildSqlSessionFactory();
	}

	@Override
	public void setResourceLoader(ResourceLoader loader) {
		this.resourceLoader = loader;
	}

}
