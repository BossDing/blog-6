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
package me.qyh.blog.core.ui;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.thymeleaf.IEngineConfiguration;
import org.thymeleaf.cache.AlwaysValidCacheEntryValidity;
import org.thymeleaf.cache.ICacheEntryValidity;
import org.thymeleaf.cache.NonCacheableCacheEntryValidity;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ITemplateResolver;
import org.thymeleaf.templateresolver.TemplateResolution;
import org.thymeleaf.templateresource.ITemplateResource;
import org.thymeleaf.templateresource.StringTemplateResource;

import me.qyh.blog.core.exception.SystemException;
import me.qyh.blog.core.service.UIService;

public class TemplateResolver implements ITemplateResolver {

	@Autowired
	private UIService uiService;

	@Override
	public String getName() {
		return this.getClass().getName();
	}

	@Override
	public Integer getOrder() {
		return 1;
	}

	@Override
	public TemplateResolution resolveTemplate(IEngineConfiguration configuration, String ownerTemplate,
			String templateName, Map<String, Object> templateResolutionAttributes) {
		if (!TemplateUtils.isTemplate(templateName)) {
			return null;
		}
		boolean cached = false;
		ITemplateResource templateResource = null;

		if (TemplateUtils.isPreview(templateName)) {
			templateResource = new TemplateResource(ParseContext.getPreview());
		}

		if (templateResource == null) {
			Template template = uiService.queryTemplate(templateName);
			if (template != null) {
				templateResource = new TemplateResource(template);
				cached = true;
			} else {
				templateResource = new StringTemplateResource("");
			}
		}

		ICacheEntryValidity cacheEntryValidity = cached ? AlwaysValidCacheEntryValidity.INSTANCE
				: NonCacheableCacheEntryValidity.INSTANCE;

		return new TemplateResolution(templateResource, false, TemplateMode.HTML, false, cacheEntryValidity);
	}

	public final class TemplateResource implements ITemplateResource {

		private final Template template;

		private TemplateResource(Template template) {
			super();
			this.template = template;
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
			return new StringReader(template.getTemplate());
		}

		@Override
		public ITemplateResource relative(String relativeLocation) {
			throw new SystemException("unsupport");
		}

		public Template getTemplate() {
			return template;
		}
	}
}
