package me.qyh.blog.plugin.sitemap;

import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;

import me.qyh.blog.core.plugin.PluginHandler;
import me.qyh.blog.core.plugin.PluginProperties;
import me.qyh.blog.core.plugin.RequestMappingRegistry;
import me.qyh.blog.plugin.sitemap.component.SiteMapSupport;

public class SiteMapPluginHandler implements PluginHandler {

	private PluginProperties pluginProperties = PluginProperties.getInstance();
	private final boolean enable = pluginProperties.get("plugin.sitemap.enable").map(Boolean::parseBoolean)
			.orElse(false);

	private SiteMapSupport siteMapSupport;

	@Override
	public void init(ApplicationContext applicationContext) throws Exception {
		this.siteMapSupport = applicationContext.getBean(SiteMapSupport.class);
	}

	@Override
	public void addRequestHandlerMapping(RequestMappingRegistry registry) throws Exception {
		if (enable) {
			registry.register(
					RequestMappingInfo.paths("sitemap.xml").methods(RequestMethod.GET)
							.produces("application/xml;charset=utf8"),
					new SiteMapController(siteMapSupport), SiteMapController.class.getDeclaredMethod("sitemap"));
		}
	}

}
