package me.qyh.blog.web.controller;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import me.qyh.blog.bean.JsonResult;
import me.qyh.blog.config.Constants;
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.exception.SystemException;
import me.qyh.blog.service.UIService;
import me.qyh.blog.ui.DataTag;
import me.qyh.blog.ui.Params;
import me.qyh.blog.ui.RenderedPage;
import me.qyh.blog.ui.page.SysPage.PageTarget;
import me.qyh.blog.web.interceptor.SpaceContext;

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

	@RequestMapping("data/{tagName}")
	@ResponseBody
	public JsonResult queryData(@PathVariable("tagName") String tagName,
			@RequestParam Map<String, String> allRequestParams) throws LogicException {
		DataTag tag;
		try {
			tag = new DataTag(URLDecoder.decode(tagName, Constants.CHARSET.name()));
		} catch (UnsupportedEncodingException e) {
			throw new SystemException(e.getMessage(), e);
		}
		for (Map.Entry<String, String> it : allRequestParams.entrySet()) {
			tag.put(it.getKey(), it.getValue());
		}
		return new JsonResult(true, uiService.queryData(tag));
	}
}
