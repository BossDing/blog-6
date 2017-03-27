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
package me.qyh.blog.web.thymeleaf;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.GsonHttpMessageConverter;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import me.qyh.blog.core.lock.LockArgumentResolver;
import me.qyh.blog.core.thymeleaf.TemplateRender;
import me.qyh.blog.core.thymeleaf.TemplateReturnValueHandler;
import me.qyh.blog.util.Jsons;

/**
 * 替代默认的RequestMappingHandlerMapping
 * 
 * @author Administrator
 *
 */
@Configuration
public class TemplateWebConfig extends WebMvcConfigurationSupport {

	@Autowired
	private TemplateRender templateRender;

	@Override
	protected RequestMappingHandlerMapping createRequestMappingHandlerMapping() {
		return new TemplateRequestMappingHandlerMapping();
	}

	@Override
	protected void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
		addDefaultHttpMessageConverters(converters);
		HttpMessageConverter<?> toRemove = null;
		for (HttpMessageConverter<?> converter : converters) {
			if (converter instanceof GsonHttpMessageConverter) {
				toRemove = converter;
				break;
			}
		}
		if (toRemove != null) {
			converters.remove(toRemove);
		}
		GsonHttpMessageConverter msgConverter = new GsonHttpMessageConverter();
		msgConverter.setGson(Jsons.getGson());
		converters.add(msgConverter);
	}

	@Override
	protected void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
		argumentResolvers.add(new LockArgumentResolver());
	}

	@Override
	protected void addReturnValueHandlers(List<HandlerMethodReturnValueHandler> returnValueHandlers) {
		returnValueHandlers.add(new TemplateReturnValueHandler(templateRender));
	}

}