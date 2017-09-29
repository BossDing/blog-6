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

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.GsonHttpMessageConverter;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import me.qyh.blog.core.util.Jsons;
import me.qyh.blog.file.store.local.FileStoreUrlHandlerMapping;
import me.qyh.blog.template.render.TemplateRender;
import me.qyh.blog.web.lock.LockArgumentResolver;
import me.qyh.blog.web.view.TemplateRequestMappingHandlerMapping;
import me.qyh.blog.web.view.TemplateReturnValueHandler;

/**
 * 替代默认的RequestMappingHandlerMapping
 * 
 * @author Administrator
 *
 */
@Configuration
public class WebConfiguration extends WebMvcConfigurationSupport {

	@Autowired
	private TemplateRender templateRender;

	private static final Integer cacheSec = 31556926;

	@Bean
	@Override
	public TemplateRequestMappingHandlerMapping requestMappingHandlerMapping() {
		return (TemplateRequestMappingHandlerMapping) super.requestMappingHandlerMapping();
	}

	@Bean
	@Override
	public FileStoreUrlHandlerMapping resourceHandlerMapping() {
		SimpleUrlHandlerMapping mapping = (SimpleUrlHandlerMapping) super.resourceHandlerMapping();
		FileStoreUrlHandlerMapping fsMapping = new FileStoreUrlHandlerMapping();
		fsMapping.setOrder(mapping.getOrder());
		fsMapping.setUrlMap(mapping.getUrlMap());
		return fsMapping;
	}

	@Override
	protected RequestMappingHandlerMapping createRequestMappingHandlerMapping() {
		TemplateRequestMappingHandlerMapping mapping = new TemplateRequestMappingHandlerMapping();
		mapping.setUseSuffixPatternMatch(false);
		return mapping;
	}

	@Override
	protected void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry.addResourceHandler("/favicon.ico").setCachePeriod(cacheSec)
				.addResourceLocations("/static/img/favicon.ico");
		registry.addResourceHandler("/static/**").setCachePeriod(cacheSec).addResourceLocations("/static/");
		registry.addResourceHandler("/doc/**").setCachePeriod(cacheSec).addResourceLocations("/doc/");
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

		// 替代默认的GsonHttpMessageConverter
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