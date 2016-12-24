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

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.thymeleaf.IEngineConfiguration;
import org.thymeleaf.cache.AlwaysValidCacheEntryValidity;
import org.thymeleaf.cache.ICacheEntryValidity;
import org.thymeleaf.cache.NonCacheableCacheEntryValidity;
import org.thymeleaf.spring4.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.templateresource.ITemplateResource;

import me.qyh.blog.exception.LogicException;
import me.qyh.blog.exception.RuntimeLogicException;
import me.qyh.blog.exception.SystemException;
import me.qyh.blog.service.UIService;
import me.qyh.blog.ui.fragment.Fragment;
import me.qyh.blog.ui.page.DisposiblePage;
import me.qyh.blog.ui.page.Page;

public class TplResolver extends SpringResourceTemplateResolver {

	@Autowired
	private UIService uiService;

	private static final String EMPTY = "empty";

	@Override
	protected String computeResourceName(IEngineConfiguration configuration, String ownerTemplate, String template,
			String prefix, String suffix, Map<String, String> templateAliases,
			Map<String, Object> templateResolutionAttributes) {
		if (TemplateUtils.isPageTemplate(template) || TemplateUtils.isFragmentTemplate(template)) {
			return template;
		}
		// 如果挂件不存在，返回空页面
		return super.computeResourceName(configuration, ownerTemplate, template, prefix, suffix, templateAliases,
				templateResolutionAttributes);
	}

	@Override
	protected ITemplateResource computeTemplateResource(IEngineConfiguration configuration, String ownerTemplate,
			String template, String resourceName, String characterEncoding,
			Map<String, Object> templateResolutionAttributes) {
		if (TemplateUtils.isDisposablePageTemplate(template)) {
			DisposiblePage page = DisposablePageContext.get();
			if (page == null) {
				throw new SystemException("DisposiblePage没有在一个上下文中");
			}
			return new PageResource(page);
		}
		if (TemplateUtils.isPageTemplate(template)) {
			try {
				Page page = uiService.queryPage(template);
				return new PageResource(page);
			} catch (LogicException e) {
				throw new RuntimeLogicException(e);
			}
		}
		if (TemplateUtils.isFragmentTemplate(template)) {
			// 这里实际上是重复查询了，但这里必定会走缓存
			Fragment fragment = uiService.queryFragment(TemplateUtils.getFragmentName(template));
			if (fragment == null) {
				template = EMPTY;
			} else {
				return new FragmentResource(fragment);
			}
		}
		return super.computeTemplateResource(configuration, ownerTemplate, template, resourceName, characterEncoding,
				templateResolutionAttributes);
	}

	@Override
	protected ICacheEntryValidity computeValidity(IEngineConfiguration configuration, String ownerTemplate,
			String template, Map<String, Object> templateResolutionAttributes) {
		if (TemplateUtils.isDisposablePageTemplate(template)) {
			return NonCacheableCacheEntryValidity.INSTANCE;
		}
		return AlwaysValidCacheEntryValidity.INSTANCE;
	}

	private final class FragmentResource implements ITemplateResource {

		private final Fragment fragment;

		public FragmentResource(Fragment fragment) {
			super();
			this.fragment = fragment;
		}

		@Override
		public String getDescription() {
			return "";
		}

		@Override
		public String getBaseName() {
			return null;
		}

		@Override
		public boolean exists() {
			return true;
		}

		@Override
		public Reader reader() throws IOException {
			return new StringReader(fragment.getTpl());
		}

		@Override
		public ITemplateResource relative(String relativeLocation) {
			throw new SystemException("un support");
		}

	}

	private final class PageResource implements ITemplateResource {

		private final Page page;

		public PageResource(Page page) {
			this.page = page;
		}

		@Override
		public String getDescription() {
			return "";
		}

		@Override
		public String getBaseName() {
			return null;
		}

		@Override
		public boolean exists() {
			return true;
		}

		@Override
		public Reader reader() throws IOException {
			return new StringReader(page.getTpl());
		}

		@Override
		public ITemplateResource relative(String relativeLocation) {
			throw new SystemException("un support");
		}
	}
}
