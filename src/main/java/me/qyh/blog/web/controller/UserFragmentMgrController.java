package me.qyh.blog.web.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import me.qyh.blog.bean.JsonResult;
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.message.Message;
import me.qyh.blog.pageparam.SpaceQueryParam;
import me.qyh.blog.pageparam.UserFragmentQueryParam;
import me.qyh.blog.service.ConfigService;
import me.qyh.blog.service.SpaceService;
import me.qyh.blog.service.UIService;
import me.qyh.blog.ui.fragment.UserFragment;
import me.qyh.blog.web.controller.form.UserFragmentQueryParamValidator;
import me.qyh.blog.web.controller.form.UserFragmentValidator;

@Controller
@RequestMapping("mgr/fragment/user")
public class UserFragmentMgrController extends BaseMgrController {

	@Autowired
	private UIService uiService;
	@Autowired
	private UserFragmentQueryParamValidator userFragmentParamValidator;
	@Autowired
	private UserFragmentValidator userFragmentValidator;
	@Autowired
	private ConfigService configService;
	@Autowired
	private SpaceService spaceService;

	@InitBinder(value = "userFragmentQueryParam")
	protected void initUserFragmentQueryParamBinder(WebDataBinder binder) {
		binder.setValidator(userFragmentParamValidator);
	}

	@InitBinder(value = "userFragment")
	protected void initUserFragmentBinder(WebDataBinder binder) {
		binder.setValidator(userFragmentValidator);
	}

	@RequestMapping("index")
	public String index(@Validated UserFragmentQueryParam userFragmentQueryParam, BindingResult result, Model model) {
		if (result.hasErrors()) {
			userFragmentQueryParam = new UserFragmentQueryParam();
			userFragmentQueryParam.setCurrentPage(1);
		}
		userFragmentQueryParam.setPageSize(configService.getPageSizeConfig().getUserFragmentPageSize());
		model.addAttribute("page", uiService.queryUserFragment(userFragmentQueryParam));
		model.addAttribute("spaces", spaceService.querySpace(new SpaceQueryParam()));
		return "mgr/fragment/user/index";
	}

	@RequestMapping("list")
	@ResponseBody
	public JsonResult listJson(@Validated UserFragmentQueryParam userFragmentQueryParam, BindingResult result,
			Model model) {
		if (result.hasErrors()) {
			userFragmentQueryParam = new UserFragmentQueryParam();
			userFragmentQueryParam.setCurrentPage(1);
		}
		userFragmentQueryParam.setPageSize(configService.getPageSizeConfig().getUserFragmentPageSize());
		return new JsonResult(true, uiService.queryUserFragment(userFragmentQueryParam));
	}

	@RequestMapping(value = "create", method = RequestMethod.POST)
	@ResponseBody
	public JsonResult create(@RequestBody @Validated final UserFragment userFragment) throws LogicException {
		if (userFragment.isGlobal())
			userFragment.setSpace(null);
		uiService.insertUserFragment(userFragment);
		return new JsonResult(true, new Message("fragment.user.create.success", "创建成功"));
	}

	@RequestMapping(value = "delete", method = RequestMethod.POST)
	@ResponseBody
	public JsonResult delete(@RequestParam("id") Integer id) throws LogicException {
		uiService.deleteUserFragment(id);
		return new JsonResult(true, new Message("fragment.user.delete.success", "删除成功"));
	}

	@RequestMapping(value = "update", method = RequestMethod.POST)
	@ResponseBody
	public JsonResult update(@RequestBody @Validated final UserFragment userFragment) throws LogicException {
		if (userFragment.isGlobal())
			userFragment.setSpace(null);
		uiService.updateUserFragment(userFragment);
		return new JsonResult(true, new Message("fragment.user.update.success", "更新成功"));
	}

	@RequestMapping(value = "get/{id}")
	@ResponseBody
	public UserFragment get(@PathVariable("id") Integer id) throws LogicException {
		return uiService.queryUserFragment(id);
	}
}
