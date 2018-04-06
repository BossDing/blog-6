package me.qyh.blog.plugin.wechat;

import java.lang.reflect.Method;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;

import me.qyh.blog.core.config.UrlHelper;
import me.qyh.blog.core.exception.SystemException;
import me.qyh.blog.core.util.FileUtils;
import me.qyh.blog.plugin.PluginHandlerAdapter;
import me.qyh.blog.plugin.PluginProperties;
import me.qyh.blog.plugin.RequestMappingRegistry;
import me.qyh.blog.plugin.wechat.WechatSupport.Signature;

public class WechatPluginHandler extends PluginHandlerAdapter {

	private WechatSupport wechatSupport;

	@Autowired
	private UrlHelper urlHelper;

	private static final String APPID_KEY = "plugin.wechat.appid";
	private static final String APPSECRET_KEY = "plugin.wechat.appsecret";
	private static final String ENABLE_KEY = "plugin.wechat.enable";

	@Override
	public void addRequestHandlerMapping(RequestMappingRegistry registry) {
		Map<String, String> map = PluginProperties.getInstance().gets(ENABLE_KEY, APPID_KEY, APPSECRET_KEY);
		if (Boolean.parseBoolean(map.get(ENABLE_KEY))) {
			wechatSupport = new WechatSupport(map.get(APPID_KEY), map.get(APPID_KEY));

			Method method = getDeclaredMethod(WechatController.class, "jsConfig", String.class);
			registry.register(RequestMappingInfo.paths("wechat/jsConfig").methods(RequestMethod.GET),
					new WechatController(), method);
		}
	}

	private Method getDeclaredMethod(Class<?> clazz, String name, Class<?>... parameterTypes) {
		try {
			return clazz.getDeclaredMethod(name, parameterTypes);
		} catch (NoSuchMethodException | SecurityException e) {
			throw new SystemException(e.getMessage(), e);
		}
	}

	public class WechatController {

		@ResponseBody
		public Signature jsConfig(@RequestParam(name = "path", defaultValue = "") String path) {
			String clean = FileUtils.cleanPath(path);
			String url = clean.isEmpty() ? urlHelper.getUrl() : urlHelper.getUrl() + '/' + clean;
			return wechatSupport.createSignature(url);
		}

	}
}
