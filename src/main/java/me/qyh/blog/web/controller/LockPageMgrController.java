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
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import me.qyh.blog.bean.JsonResult;
import me.qyh.blog.config.Constants;
import me.qyh.blog.entity.Space;
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.message.Message;
import me.qyh.blog.service.UIService;
import me.qyh.blog.ui.TplRenderException;
import me.qyh.blog.ui.UIRender;
import me.qyh.blog.ui.TemplateUtils;
import me.qyh.blog.ui.page.DisposiblePage;
import me.qyh.blog.ui.page.LockPage;
import me.qyh.blog.web.controller.form.PageValidator;

@RequestMapping("mgr/page/lock")
@Controller
public class LockPageMgrController extends BaseMgrController {

	@Autowired
	private UIService uiService;
	@Autowired
	private UIRender uiRender;

	@Autowired
	private PageValidator pageValidator;

	@InitBinder(value = "lockPage")
	protected void initBinder(WebDataBinder binder) {
		binder.setValidator(pageValidator);
	}

	@RequestMapping(value = "build", method = RequestMethod.GET)
	public String build(@RequestParam("lockType") String lockType,
			@RequestParam(required = false, value = "spaceId") Integer spaceId, Model model) throws LogicException {
		model.addAttribute("page", uiService.queryPage(
				TemplateUtils.getTemplateName(new LockPage(spaceId == null ? null : new Space(spaceId), lockType))));
		return "mgr/page/lock/build";
	}

	@RequestMapping(value = "build", method = RequestMethod.POST)
	@ResponseBody
	public JsonResult build(@RequestBody @Validated LockPage lockPage, HttpServletRequest request,
			HttpServletResponse response) throws LogicException {
		uiService.buildTpl(lockPage);
		return new JsonResult(true, new Message("page.lock.build.success", "保存成功"));
	}

	@RequestMapping(value = "preview", method = RequestMethod.POST)
	@ResponseBody
	public JsonResult preview(@RequestBody @Validated LockPage lockPage, HttpServletRequest request,
			HttpServletResponse response) throws LogicException {
		String rendered;
		try {
			rendered = uiRender.render(new DisposiblePage(lockPage), request, response);
			request.getSession().setAttribute(Constants.TEMPLATE_PREVIEW_KEY, rendered);
			return new JsonResult(true, rendered);
		} catch (TplRenderException e) {
			return new JsonResult(false, e.getRenderErrorDescription());
		}
	}

	@RequestMapping(value = "delete", method = RequestMethod.POST)
	@ResponseBody
	public JsonResult delete(@RequestParam("lockType") String lockType,
			@RequestParam(required = false, value = "spaceId") Integer spaceId) throws LogicException {
		uiService.deleteLockPage(spaceId, lockType);
		return new JsonResult(true, new Message("page.lock.delete.success", "还原成功"));
	}

}
