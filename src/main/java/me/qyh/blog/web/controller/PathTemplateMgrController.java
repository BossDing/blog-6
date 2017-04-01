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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.support.RequestContextUtils;

import me.qyh.blog.core.bean.JsonResult;
import me.qyh.blog.core.config.Constants;
import me.qyh.blog.core.entity.Space;
import me.qyh.blog.core.exception.LogicException;
import me.qyh.blog.core.security.Environment;
import me.qyh.blog.core.service.SpaceService;
import me.qyh.blog.core.thymeleaf.TemplateRender;
import me.qyh.blog.core.thymeleaf.TemplateService;
import me.qyh.blog.core.thymeleaf.TplRenderException;
import me.qyh.blog.core.thymeleaf.template.PathTemplate;
import me.qyh.blog.util.UrlUtils;
import me.qyh.blog.util.Validators;

@Controller
@RequestMapping("mgr/template/path")
public class PathTemplateMgrController extends BaseMgrController {
	@Autowired
	private TemplateService templateService;
	@Autowired
	private TemplateRender templateRender;
	@Autowired
	private SpaceService spaceService;

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
	public String preview(@RequestParam("path") String path, HttpServletRequest request, HttpServletResponse response)
			throws LogicException {
		// 设置空间
		PathTemplate preview = templateService.getPathTemplateService().getPreview(path);
		String alias = getSpaceAliasFromPath(path);
		Space space = spaceService.getSpace(alias).orElse(null);
		Environment.setSpace(space);
		try {
			request.getSession().setAttribute(Constants.TEMPLATE_PREVIEW_KEY,
					templateRender.renderPreview(preview, request, response));
			return "redirect:/mgr/template/preview";
		} catch (TplRenderException e) {
			RequestContextUtils.getOutputFlashMap(request).put("description", e.getRenderErrorDescription());
			return "redirect:/error/ui";
		} finally {
			Environment.setSpace(null);
		}
	}

	private String getSpaceAliasFromPath(String path) {
		if (UrlUtils.match("space/*/**", path)) {
			String alias = path.split("/")[1];
			if (Validators.isLetterOrNum(alias)) {
				return alias;
			}
		}
		return null;
	}

}
