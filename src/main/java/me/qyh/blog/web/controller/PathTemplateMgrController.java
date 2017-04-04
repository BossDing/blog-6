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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import me.qyh.blog.core.bean.JsonResult;
import me.qyh.blog.core.config.UrlHelper;
import me.qyh.blog.core.exception.LogicException;
import me.qyh.blog.core.thymeleaf.TemplateService;
import me.qyh.blog.util.FileUtils;

@Controller
@RequestMapping("mgr/template/path")
public class PathTemplateMgrController extends BaseMgrController {
	@Autowired
	private TemplateService templateService;
	@Autowired
	private UrlHelper urlHelper;

	@RequestMapping(value = "index", method = RequestMethod.GET)
	public String index(ModelMap model, @RequestParam(value = "pattern", required = false) String pattern) {
		try {
			model.addAttribute("templates", templateService.getPathTemplateService().queryPathTemplates(pattern));
		} catch (LogicException e) {
			model.addAttribute(ERROR, e.getLogicMessage());
		}
		return "mgr/template/path";
	}

	@RequestMapping(value = "reload", method = RequestMethod.GET)
	@ResponseBody
	public JsonResult reload(@RequestParam("path") String path) throws LogicException {
		return new JsonResult(true, templateService.getPathTemplateService().loadPathTemplateFile(path));
	}

	@RequestMapping(value = "preview", method = RequestMethod.GET)
	public String preview(@RequestParam("path") String path, ModelMap model) throws LogicException {
		// 设置空间
		templateService.getPathTemplateService().registerPreview(path);
		PreviewUrl url = new PreviewUrl(urlHelper.getUrl() + "/" + FileUtils.cleanPath(path));
		if (url.isHasPathVariable()) {
			return "redirect:" + url.getUrl();
		}
		model.put("url", url.getUrl());
		return "mgr/template/preview";
	}
}
