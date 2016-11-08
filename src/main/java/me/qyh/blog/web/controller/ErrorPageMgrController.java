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
import me.qyh.blog.ui.RenderedPage;
import me.qyh.blog.ui.TplRender;
import me.qyh.blog.ui.TplRenderException;
import me.qyh.blog.ui.page.ErrorPage;
import me.qyh.blog.ui.page.ErrorPage.ErrorCode;
import me.qyh.blog.web.controller.form.PageValidator;

@RequestMapping("mgr/page/error")
@Controller
public class ErrorPageMgrController extends BaseMgrController {

	@Autowired
	private UIService uiService;
	@Autowired
	private TplRender tplRender;

	@Autowired
	private PageValidator pageValidator;

	@InitBinder(value = "errorPage")
	protected void initBinder(WebDataBinder binder) {
		binder.setValidator(pageValidator);
	}

	@RequestMapping(value = "build", method = RequestMethod.GET)
	public String build(@RequestParam("errorCode") ErrorCode code,
			@RequestParam(required = false, value = "spaceId") Integer spaceId, Model model) {
		model.addAttribute("page", uiService.queryErrorPage(spaceId == null ? null : new Space(spaceId), code));
		return "mgr/page/error/build";
	}

	@RequestMapping(value = "build", method = RequestMethod.POST)
	@ResponseBody
	public JsonResult build(@RequestBody @Validated ErrorPage errorPage, HttpServletRequest request,
			HttpServletResponse response) throws LogicException {
		RenderedPage page = uiService.renderPreviewPage(errorPage);
		if (ErrorCode.ERROR_200.equals(errorPage.getErrorCode())) {
			request.setAttribute("error", new Message("error.200", "200"));
		}
		try {
			tplRender.tryRender(page, request, response);
		} catch (TplRenderException e) {
			return new JsonResult(false, e.getRenderErrorDescription());
		}
		uiService.buildTpl(errorPage);
		return new JsonResult(true, new Message("page.error.build.success", "保存成功"));
	}

	@RequestMapping(value = "preview", method = RequestMethod.POST)
	@ResponseBody
	public JsonResult preview(@RequestBody @Validated ErrorPage errorPage, HttpServletRequest request,
			HttpServletResponse response) throws LogicException {
		try {
			RenderedPage page = uiService.renderPreviewPage(errorPage);
			if (ErrorCode.ERROR_200.equals(errorPage.getErrorCode())) {
				request.setAttribute("error", new Message("error.200", "200"));
			}
			String rendered = tplRender.tryRender(page, request, response);
			request.getSession().setAttribute(Constants.TEMPLATE_PREVIEW_KEY, rendered);
			return new JsonResult(true, rendered);
		} catch (TplRenderException e) {
			return new JsonResult(false, e.getRenderErrorDescription());
		}
	}

	@RequestMapping(value = "delete", method = RequestMethod.POST)
	@ResponseBody
	public JsonResult delete(@RequestParam("errorCode") ErrorCode code,
			@RequestParam(required = false, value = "spaceId") Integer spaceId) throws LogicException {
		uiService.deleteErrorPage(spaceId == null ? null : new Space(spaceId), code);
		return new JsonResult(true, new Message("page.error.delete.success", "还原成功"));
	}

}
