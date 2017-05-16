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
			Object handler = event.getHandler();
			if (handler != null) {
				registerHandler(event.getPath(), handler);
			}
		}

	}

}
