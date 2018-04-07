package me.qyh.blog.plugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternUtils;

import me.qyh.blog.core.config.Constants;
import me.qyh.blog.core.exception.SystemException;
import me.qyh.blog.core.util.Validators;

public class PluginSqlSessionFactoryBean extends SqlSessionFactoryBean implements ResourceLoaderAware {

	private ResourceLoader resourceLoader;

	@Override
	protected SqlSessionFactory buildSqlSessionFactory() throws IOException {

		ResourcePatternResolver resolver = ResourcePatternUtils.getResourcePatternResolver(resourceLoader);

		Resource[] mapperLocations = resolver.getResources("classpath:me/qyh/blog/plugin/*/mapper/*.xml");

		if (!Validators.isEmpty(mapperLocations)) {
			super.setMapperLocations(mapperLocations);
		}

		Resource[] typeAliasResources = resolver.getResources("classpath:me/qyh/blog/plugin/*/mapper/typeAlias.txt");
		if (!Validators.isEmpty(typeAliasResources)) {
			Set<Class<?>> classSet = new HashSet<>();
			for (Resource typeAliasResource : typeAliasResources) {
				try (InputStream is = typeAliasResource.getInputStream();
						BufferedReader reader = new BufferedReader(new InputStreamReader(is, Constants.CHARSET))) {
					reader.lines().forEach(line -> {

						if (!Validators.isEmptyOrNull(line, true)) {
							try {
								classSet.add(Class.forName(line));
							} catch (ClassNotFoundException e) {
								throw new SystemException(e.getMessage(), e);
							}

						}

					});
				}
			}
			if (!classSet.isEmpty()) {
				super.setTypeAliases(classSet.toArray(new Class<?>[classSet.size()]));
			}
		}

		return super.buildSqlSessionFactory();
	}

	@Override
	public void setResourceLoader(ResourceLoader loader) {
		this.resourceLoader = loader;
	}

}
