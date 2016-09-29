package me.qyh.blog.web.controller;

import java.net.URLDecoder;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import me.qyh.blog.bean.JsonResult;
import me.qyh.blog.config.Constants;
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.service.UIService;
import me.qyh.blog.ui.Params;
import me.qyh.blog.ui.RenderedPage;
import me.qyh.blog.ui.page.SysPage.PageTarget;
import me.qyh.blog.web.interceptor.SpaceContext;
import me.qyh.util.UrlUtils;

@Controller
@RequestMapping("space/{alias}")
public class SpaceController extends BaseController {

	@Autowired
	private UIService uiService;

	@RequestMapping(value = { "/", "" })
	public RenderedPage index() throws LogicException {
		return uiService.renderSysPage(SpaceContext.get(), PageTarget.INDEX, new Params());
	}

	@RequestMapping("page/{alias}")
	public RenderedPage userPage(@PathVariable("alias") String alias) throws LogicException {
		return uiService.renderUserPage(alias);
	}
	
	@RequestMapping("data/**")
	@ResponseBody
	public JsonResult queryData(HttpServletRequest request) throws LogicException {
		String dataTagStr = null;
		try {
			String fullUrl = UrlUtils.buildFullRequestUrl(request);
			dataTagStr = URLDecoder.decode(fullUrl.substring(fullUrl.indexOf("/data/") + 6), Constants.CHARSET.name());
		} catch (Exception e) {
		}
		if (dataTagStr != null)
			return new JsonResult(true, uiService.queryData(dataTagStr));
		return new JsonResult(true, "");
	}
}
