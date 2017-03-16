package me.qyh.blog.web;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationListener;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;

@Component
public class AppSimpleUrlHandlerMapping extends SimpleUrlHandlerMapping implements InitializingBean {

	@Override
	public void afterPropertiesSet() throws Exception {
		((AbstractApplicationContext) getApplicationContext().getParent())
				.addApplicationListener(new SimpleUrlMappingRegisterEventListener());
	}

	private final class SimpleUrlMappingRegisterEventListener
			implements ApplicationListener<SimpleUrlMappingRegisterEvent> {

		@Override
		public void onApplicationEvent(SimpleUrlMappingRegisterEvent event) {
			registerHandler(event.getPath(), event.getHandler());
		}

	}

}
