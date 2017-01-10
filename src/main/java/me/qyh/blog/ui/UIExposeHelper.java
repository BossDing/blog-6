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
package me.qyh.blog.ui;

import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.CollectionUtils;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import me.qyh.blog.config.UrlHelper;
import me.qyh.blog.message.Messages;
import me.qyh.blog.security.Environment;
import me.qyh.blog.ui.utils.DefaultTimeDiffParser;
import me.qyh.blog.ui.utils.TimeDiffParser;
import me.qyh.blog.ui.utils.UIUtils;
import me.qyh.blog.util.Validators;

public class UIExposeHelper implements InitializingBean {

	@Autowired
	private UrlHelper urlHelper;
	@Autowired
	private Messages messages;
	@Autowired(required = false)
	private TimeDiffParser timeDiffParser;

	/**
	 * 用于扫描 UIUtils
	 */
	private String[] packages;

	private Map<String, Object> pros = Maps.newHashMap();

	public final void addVariables(HttpServletRequest request) {
		if (!CollectionUtils.isEmpty(pros)) {
			for (Map.Entry<String, Object> it : pros.entrySet()) {
				request.setAttribute(it.getKey(), it.getValue());
			}
		}
		request.setAttribute("urls", urlHelper.getUrls(request));
		request.setAttribute("user", Environment.getUser().orElse(null));
		request.setAttribute("messages", messages);
		request.setAttribute("space", Environment.getSpace().orElse(null));
		request.setAttribute("timeDiffParser", timeDiffParser);
	}

	public void setPros(Map<String, Object> pros) {
		this.pros = pros;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (timeDiffParser == null) {
			timeDiffParser = new DefaultTimeDiffParser();
		}
		ClassPathScanningCandidateComponentProvider scanner = new ClassPathScanningCandidateComponentProvider(false);
		scanner.addIncludeFilter(new AnnotationTypeFilter(UIUtils.class));
		Set<BeanDefinition> definitions = Sets.newHashSet();
		definitions.addAll(scanner.findCandidateComponents("me.qyh.blog"));
		if (packages != null && packages.length > 0) {
			for (String pack : packages) {
				definitions.addAll(scanner.findCandidateComponents(pack));
			}
		}
		for (BeanDefinition definition : definitions) {
			Class<?> clazz = Class.forName(definition.getBeanClassName());
			UIUtils ann = AnnotationUtils.findAnnotation(clazz, UIUtils.class);
			String name = ann.name();
			if (Validators.isEmptyOrNull(name, true)) {
				name = clazz.getSimpleName();
				name = Character.toLowerCase(name.charAt(0)) + name.substring(1);
			}
			pros.put(name, clazz);
		}
	}

	public void setPackages(String[] packages) {
		this.packages = packages;
	}
}
