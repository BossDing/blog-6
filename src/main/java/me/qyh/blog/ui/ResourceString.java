package me.qyh.blog.ui;

import java.io.IOException;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.core.io.Resource;

import me.qyh.blog.util.Resources;

/**
 * 将Resource转化为String
 * 
 * @author mhlx
 *
 */
public class ResourceString implements FactoryBean<String> {

	private final String str;

	public ResourceString(Resource resource) throws IOException {
		this.str = Resources.readResourceToString(resource);
	}

	@Override
	public String getObject() throws Exception {
		return str;
	}

	@Override
	public Class<?> getObjectType() {
		return String.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
