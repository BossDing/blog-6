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
package me.qyh.blog.web.controller;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FilenameUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import me.qyh.blog.bean.JsonResult;
import me.qyh.blog.config.Constants;
import me.qyh.blog.entity.Space;
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.lock.LockManager;
import me.qyh.blog.message.Message;
import me.qyh.blog.pageparam.SpaceQueryParam;
import me.qyh.blog.service.SpaceService;
import me.qyh.blog.service.UIService;
import me.qyh.blog.ui.RenderedPage;
import me.qyh.blog.ui.TplRender;
import me.qyh.blog.ui.TplRenderException;
import me.qyh.blog.ui.page.SysPage;
import me.qyh.blog.ui.page.ErrorPage.ErrorCode;
import me.qyh.blog.ui.page.SysPage.PageTarget;
import me.qyh.blog.web.controller.form.PageValidator;

@RequestMapping("mgr/page/sys")
@Controller
public class SysPageMgrController extends BaseMgrController {

	@Autowired
	private UIService uiService;
	@Autowired
	private SpaceService spaceService;
	@Autowired
	private TplRender tplRender;
	@Autowired
	private LockManager lockManager;

	@Autowired
	private PageValidator pageValidator;

	@InitBinder(value = "sysPage")
	protected void initBinder(WebDataBinder binder) {
		binder.setValidator(pageValidator);
	}

	@RequestMapping(value = "index", method = RequestMethod.GET)
	public String index(Model model, RedirectAttributes ra) {
		SpaceQueryParam param = new SpaceQueryParam();
		List<Space> spaces = spaceService.querySpace(param);
		model.addAttribute("spaces", spaces);
		model.addAttribute("errorCodes", ErrorCode.values());
		model.addAttribute("pageTargets", PageTarget.values());
		model.addAttribute("lockTypes", lockManager.allTypes());
		return "mgr/page/sys/index";
	}

	@RequestMapping(value = "build", method = RequestMethod.GET)
	public String build(@RequestParam("target") PageTarget target,
			@RequestParam(required = false, value = "spaceId") Integer spaceId, Model model) {
		model.addAttribute("page", uiService.querySysPage(spaceId == null ? null : new Space(spaceId), target));
		return "mgr/page/sys/build";
	}

	@RequestMapping(value = "build", method = RequestMethod.POST)
	@ResponseBody
	public JsonResult build(@RequestBody @Validated SysPage sysPage, HttpServletRequest request,
			HttpServletResponse response) throws LogicException {
		RenderedPage page = uiService.renderPreviewPage(sysPage);
		try {
			tplRender.tryRender(page, request, response);
		} catch (TplRenderException e) {
			return new JsonResult(false, e.getRenderErrorDescription());
		}
		uiService.buildTpl(sysPage);
		return new JsonResult(true, new Message("page.sys.build.success", "保存成功"));
	}

	@RequestMapping(value = "preview", method = RequestMethod.POST)
	@ResponseBody
	public JsonResult preview(@RequestBody @Validated SysPage sysPage, HttpServletRequest request,
			HttpServletResponse response) throws LogicException {
		try {
			RenderedPage page = uiService.renderPreviewPage(sysPage);
			String rendered = tplRender.tryRender(page, request, response);
			request.getSession().setAttribute(Constants.TEMPLATE_PREVIEW_KEY, rendered);
			return new JsonResult(true, rendered);
		} catch (TplRenderException e) {
			return new JsonResult(false, e.getRenderErrorDescription());
		}
	}

	@RequestMapping(value = "delete", method = RequestMethod.POST)
	@ResponseBody
	public JsonResult delete(@RequestParam("target") PageTarget target,
			@RequestParam(required = false, value = "spaceId") Integer spaceId) throws LogicException {
		uiService.deleteSysPage(spaceId == null ? null : new Space(spaceId), target);
		return new JsonResult(true, new Message("page.sys.delete.success", "还原成功"));
	}

	/**
	 * 获取预览页面中的css链接和style标签内容，主要撰写博客的时候使用
	 * <p>
	 * <strong>如果某些css是动态变化的，比如根据时间和用户展现不同的css，那么指挥获取符合这一时刻的预览css(如果css的路径跟widget中的对象有关，那么只会也只能采用系统的预览widget)</strong>
	 * </p>
	 * 
	 * @param target
	 * @param spaceId
	 * @return
	 * @throws LogicException
	 */
	@RequestMapping(value = "getStyles", method = RequestMethod.GET)
	@ResponseBody
	public JsonResult getStyles(Space space, HttpServletRequest request, HttpServletResponse response)
			throws LogicException {
		RenderedPage page = uiService.renderPreviewPage(space, PageTarget.ARTICLE_DETAIL);
		try {
			String rendered = tplRender.tryRender(page, request, response);
			Document doc = Jsoup.parse(rendered);
			String style = null;
			Elements eles = doc.select("style");
			if (!eles.isEmpty()) {
				style = eles.first().data();
			}
			Set<String> csses = new LinkedHashSet<String>();
			Elements imports = doc.select("link[href]");
			for (Element ele : imports) {
				String link = ele.attr("href");
				if (isCss(link)) {
					csses.add(link);
				}
			}
			Map<String, Object> resultMap = new HashMap<>();
			resultMap.put("csses", csses);
			if (style != null)
				resultMap.put("style", style.trim());
			return new JsonResult(true, resultMap);
		} catch (TplRenderException e) {
			return new JsonResult(false);
		}
	}

	private static boolean isCss(String link) {
		String ext = FilenameUtils.getExtension(link);
		if (ext.equalsIgnoreCase("css")) {
			return true;
		} else {
			int idx = ext.indexOf('?');
			if (idx != -1) {
				ext = ext.substring(0, idx);
				if (ext.equalsIgnoreCase("css")) {
					return true;
				}
			}
		}
		return false;
	}
}
