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

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.google.common.collect.Maps;

import me.qyh.blog.bean.JsonResult;
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.service.UIService;
import me.qyh.blog.ui.ContextVariables;
import me.qyh.blog.ui.DataTag;
import me.qyh.blog.ui.TplRenderException;
import me.qyh.blog.ui.UIRender;
import me.qyh.blog.ui.data.DataBind;
import me.qyh.blog.ui.fragment.Fragment;
import me.qyh.blog.ui.page.DisposiblePage;
import me.qyh.blog.ui.page.Page;
import me.qyh.blog.ui.page.SysPage;
import me.qyh.blog.ui.page.SysPage.PageTarget;
import me.qyh.blog.web.Webs;
import me.qyh.blog.web.interceptor.SpaceContext;

@Controller
public class IndexController {

	@Autowired
	private UIRender uiRender;
	@Autowired
	private UIService uiService;

	@RequestMapping(value = { "/", "space/{alias}/", "", "space/{alias}" })
	public Page index() throws LogicException {
		return new SysPage(SpaceContext.get(), PageTarget.INDEX);
	}

	@RequestMapping(value = { "data/{tagName}",
			"space/{alias}/data/{tagName}" }, method = RequestMethod.GET, headers = { "X-Fragment" })
	@ResponseBody
	public JsonResult queryData(@PathVariable("tagName") String tagName,
			@RequestParam Map<String, String> allRequestParams,
			@RequestHeader(value = "X-Fragment", required = true) String fragment, HttpServletRequest request,
			HttpServletResponse response) throws LogicException {
		DisposiblePage page = new DisposiblePage();
		page.setPreview(false);
		page.setTpl(buildTag("data", Webs.decode(tagName), allRequestParams)
				+ buildTag("fragment", Webs.decode(fragment), null));
		try {
			return new JsonResult(true, uiRender.render(page, request, response));
		} catch (TplRenderException e) {
			return new JsonResult(false, e.getRenderErrorDescription());
		}
	}

	@RequestMapping(value = { "data/{tagName}", "space/{alias}/data/{tagName}" }, method = RequestMethod.GET)
	@ResponseBody
	public JsonResult queryData(@PathVariable("tagName") String tagName,
			@RequestParam Map<String, String> allRequestParams, HttpServletRequest request,
			HttpServletResponse response) throws LogicException {
		Map<String, String> attMap = Maps.newHashMap();
		for (Map.Entry<String, String> it : allRequestParams.entrySet()) {
			attMap.put(it.getKey(), it.getValue());
		}
		DataTag tag = new DataTag(Webs.decode(tagName), attMap);
		DataBind<?> result = uiService.queryData(tag, new ContextVariables());
		return new JsonResult(true, result != null ? result : null);
	}

	@RequestMapping(value = { "fragment/{fragment}", "space/{alias}/fragment/{fragment}" }, method = RequestMethod.GET)
	@ResponseBody
	public JsonResult queryFragment(@PathVariable("fragment") String fragment,
			@RequestParam Map<String, String> allRequestParams, HttpServletRequest request,
			HttpServletResponse response) throws LogicException {
		Fragment fr = uiService.queryFragment(Webs.decode(fragment));
		if (fr == null) {
			return new JsonResult(true);
		}
		DisposiblePage page = new DisposiblePage();
		page.setTpl(buildTag("fragment", fr.getName(), allRequestParams));
		Map<String, Fragment> frMap = Maps.newHashMap();
		frMap.put(fr.getName(), fr);
		try {
			return new JsonResult(true, uiRender.render(page, request, response));
		} catch (TplRenderException e) {
			return new JsonResult(false, e.getRenderErrorDescription());
		}
	}

	private String buildTag(String tagName, String nameAtt, Map<String, String> atts) {
		Tag tag = Tag.valueOf(tagName);
		Attributes attributes = new Attributes();
		if (!CollectionUtils.isEmpty(atts)) {
			for (Map.Entry<String, String> it : atts.entrySet()) {
				attributes.put(it.getKey(), it.getValue());
			}
		}
		attributes.put("name", nameAtt);
		Element ele = new Element(tag, "", attributes);
		return ele.toString();
	}
}
