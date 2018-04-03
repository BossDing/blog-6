package me.qyh.blog.expanded.wechat;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import me.qyh.blog.core.config.UrlHelper;
import me.qyh.blog.core.util.FileUtils;
import me.qyh.blog.expanded.wechat.WechatSupport.Signature;

@Controller
@RequestMapping("wechat")
public class WechatController implements InitializingBean {

	private final String appid;
	private final String appsecret;

	private WechatSupport support;

	@Autowired
	private UrlHelper urlHelper;

	public WechatController(String appid, String appsecret) {
		super();
		this.appid = appid;
		this.appsecret = appsecret;
	}

	@GetMapping("jsConfig")
	@ResponseBody
	public Signature config(@RequestParam(name = "path", defaultValue = "") String path) {
		String clean = FileUtils.cleanPath(path);
		String url = clean.isEmpty() ? urlHelper.getUrl() : urlHelper.getUrl() + '/' + clean;
		return support.createSignature(url);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		this.support = new WechatSupport(appid, appsecret);
	}

}
