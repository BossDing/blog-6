package me.qyh.blog.web;

import java.util.Map;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;

public final class AppSimpleUrlHandlerMapping extends SimpleUrlHandlerMapping implements InitializingBean {

	@Autowired
	private MappingRegister mappingRegister;

	@Override
	public void afterPropertiesSet() throws Exception {
		for (Map.Entry<String, Object> it : mappingRegister.getMapping().entrySet()) {
			super.registerHandler(it.getKey(), it.getValue());
		}
	}

}
