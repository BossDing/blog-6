package me.qyh.blog.ui;

import java.util.Map;

import org.springframework.core.io.ByteArrayResource;
import org.thymeleaf.IEngineConfiguration;
import org.thymeleaf.cache.ICacheEntryValidity;
import org.thymeleaf.cache.NonCacheableCacheEntryValidity;
import org.thymeleaf.spring4.templateresolver.SpringResourceTemplateResolver;
import org.thymeleaf.spring4.templateresource.SpringResourceTemplateResource;
import org.thymeleaf.templateresource.ITemplateResource;

public class TplResolver extends SpringResourceTemplateResolver {

	@Override
	protected String computeResourceName(IEngineConfiguration configuration, String ownerTemplate, String template,
			String prefix, String suffix, Map<String, String> templateAliases,
			Map<String, Object> templateResolutionAttributes) {
		Template tpl = UIContext.get();
		if (tpl != null && tpl.find(template) != null) {
			return template;
		}
		return super.computeResourceName(configuration, ownerTemplate, template, prefix, suffix, templateAliases,
				templateResolutionAttributes);
	}

	@Override
	protected ITemplateResource computeTemplateResource(IEngineConfiguration configuration, String ownerTemplate,
			String template, String resourceName, String characterEncoding,
			Map<String, Object> templateResolutionAttributes) {
		Template tpl = UIContext.get();
		if (tpl != null) {
			Template finalTpl = tpl;
			// 如果不是同一个模板
			if (!tpl.getTemplateName().equals(template)) {
				finalTpl = tpl.find(template);
			}
			if (finalTpl != null) {
				return new SpringResourceTemplateResource(
						new ByteArrayResource(finalTpl.getTpl().getBytes(), finalTpl.getTemplateName()),
						characterEncoding);
			}
		}
		return super.computeTemplateResource(configuration, ownerTemplate, template, resourceName, characterEncoding,
				templateResolutionAttributes);
	}

	// TODO cache
	@Override
	protected ICacheEntryValidity computeValidity(IEngineConfiguration configuration, String ownerTemplate,
			String template, Map<String, Object> templateResolutionAttributes) {
		// return AlwaysValidCacheEntryValidity.INSTANCE;
		return NonCacheableCacheEntryValidity.INSTANCE;
	}

}
