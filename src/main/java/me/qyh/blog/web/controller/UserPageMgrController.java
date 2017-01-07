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

import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
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
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.message.Message;
import me.qyh.blog.pageparam.SpaceQueryParam;
import me.qyh.blog.pageparam.UserPageQueryParam;
import me.qyh.blog.service.SpaceService;
import me.qyh.blog.service.UIService;
import me.qyh.blog.ui.TplRenderException;
import me.qyh.blog.ui.UIRender;
import me.qyh.blog.ui.page.DisposiblePage;
import me.qyh.blog.ui.page.UserPage;
import me.qyh.blog.web.controller.form.PageValidator;
import me.qyh.blog.web.controller.form.UserPageQueryParamValidator;

@Controller
@RequestMapping("mgr/page/user")
public class UserPageMgrController extends BaseMgrController {

	@Autowired
	private UserPageQueryParamValidator userPageParamValidator;
	@Autowired
	private UIService uiService;
	@Autowired
	private SpaceService spaceService;
	@Autowired
	private PageValidator pageValidator;
	@Autowired
	private UIRender uiRender;

	@InitBinder(value = "userPage")
	protected void initBinder(WebDataBinder binder) {
		binder.setValidator(pageValidator);
	}

	@InitBinder(value = "userPageQueryParam")
	protected void initUserPageQueryParamBinder(WebDataBinder binder) {
		binder.setValidator(userPageParamValidator);
	}

	@RequestMapping("index")
	public String index(@Validated UserPageQueryParam param, BindingResult result, Model Model) {
		if (result.hasErrors()) {
			param = new UserPageQueryParam();
			param.setCurrentPage(1);
		}
		Model.addAttribute("page", uiService.queryUserPage(param));
		return "mgr/page/user/index";
	}

	@RequestMapping(value = "build", method = RequestMethod.POST)
	@ResponseBody
	public JsonResult build(@RequestBody @Validated UserPage userPage, HttpServletRequest request,
			HttpServletResponse response) throws LogicException {
		uiService.buildTpl(userPage);
		return new JsonResult(true, new Message("page.user.build.success", "保存成功"));
	}

	@RequestMapping(value = "new")
	public String build(Model model) {
		model.addAttribute("page", new UserPage());
		SpaceQueryParam param = new SpaceQueryParam();
		model.addAttribute("spaces", spaceService.querySpace(param));
		return "mgr/page/user/build";
	}

	@RequestMapping(value = "update")
	public String update(@RequestParam("id") Integer id, Model model, RedirectAttributes ra) {
		Optional<UserPage> optional = uiService.queryUserPage(id);
		if (!optional.isPresent()) {
			ra.addFlashAttribute(ERROR, new Message("page.user.notExists", "自定义页面不存在"));
			return "redirect:/mgr/page/user/index";
		}
		UserPage page = optional.get();
		model.addAttribute("page", page);
		SpaceQueryParam param = new SpaceQueryParam();
		model.addAttribute("spaces", spaceService.querySpace(param));
		return "mgr/page/user/build";
	}

	@RequestMapping(value = "preview", method = RequestMethod.POST)
	@ResponseBody
	public JsonResult preview(@RequestBody @Validated UserPage userPage, HttpServletRequest request,
			HttpServletResponse response) throws LogicException {
		String rendered;
		try {
			rendered = uiRender.render(new DisposiblePage(userPage), request, response);
			request.getSession().setAttribute(Constants.TEMPLATE_PREVIEW_KEY, rendered);
			return new JsonResult(true, rendered);
		} catch (TplRenderException e) {
			return new JsonResult(false, e.getRenderErrorDescription());
		}
	}

	@RequestMapping(value = "delete", method = RequestMethod.POST)
	@ResponseBody
	public JsonResult delete(@RequestParam("id") Integer id) throws LogicException {
		uiService.deleteUserPage(id);
		return new JsonResult(true, new Message("page.user.delete.success", "删除成功"));
	}

}
