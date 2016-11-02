package me.qyh.blog.web.controller;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import me.qyh.blog.bean.JsonResult;
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.service.UIService;
import me.qyh.blog.ui.DataTag;
import me.qyh.blog.ui.Params;
import me.qyh.blog.ui.RenderedPage;
import me.qyh.blog.ui.TemplateParser;
import me.qyh.blog.ui.TplRender;
import me.qyh.blog.ui.TplRenderException;
import me.qyh.blog.ui.data.DataBind;
import me.qyh.blog.ui.fragment.Fragment;
import me.qyh.blog.ui.page.Page;
import me.qyh.blog.ui.page.SysPage.PageTarget;
import me.qyh.blog.web.interceptor.SpaceContext;

@Controller
@RequestMapping("space/{alias}")
public class SpaceController extends BaseController {

	@Autowired
	private UIService uiService;
	@Autowired
	private TplRender render;

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
			@RequestParam Map<String, String> allRequestParams,
			@RequestHeader(value = "X-Fragment", required = false) String fragment, HttpServletRequest request,
			HttpServletResponse response) throws LogicException {
		boolean pjax = fragment != null;
		Fragment fr = null;
		if (pjax) {
			fr = uiService.queryFragment(Webs.decode(fragment));
			if (fr == null)
				return new JsonResult(true);
		}
		DataTag tag = new DataTag(Webs.decode(tagName));
		for (Map.Entry<String, String> it : allRequestParams.entrySet()) {
			tag.put(it.getKey().toLowerCase(), it.getValue());
		}
		DataBind<?> result = uiService.queryData(tag);
		if (pjax) {
			Page page = new Page();
			page.setTpl(TemplateParser.buildFragmentTag(fr.getName(), null));
			Map<String, Fragment> frMap = new HashMap<String, Fragment>();
			frMap.put(fr.getName(), fr);
			RenderedPage rp = new RenderedPage(page, result == null ? Collections.emptyList() : Arrays.asList(result),
					frMap);
			try {
				return new JsonResult(true, render.tryRender(rp, request, response));
			} catch (TplRenderException e) {
				return new JsonResult(false, e.getRenderErrorDescription());
			}
		} else {
			return new JsonResult(true, result != null ? result : null);
		}
	}

	@RequestMapping("fragment/{fragment}")
	@ResponseBody
	public JsonResult queryFragment(@PathVariable("fragment") String fragment,
			@RequestParam Map<String, String> allRequestParams, HttpServletRequest request,
			HttpServletResponse response) throws LogicException {
		Fragment fr = uiService.queryFragment(Webs.decode(fragment));
		if (fr == null)
			return new JsonResult(true);
		Page page = new Page();
		page.setTpl(TemplateParser.buildFragmentTag(fr.getName(),allRequestParams));
		Map<String, Fragment> frMap = new HashMap<String, Fragment>();
		frMap.put(fr.getName(), fr);
		RenderedPage rp = new RenderedPage(page, Collections.emptyList(), frMap);
		try {
			return new JsonResult(true, render.tryRender(rp, request, response));
		} catch (TplRenderException e) {
			return new JsonResult(false, e.getRenderErrorDescription());
		}
	}
}
