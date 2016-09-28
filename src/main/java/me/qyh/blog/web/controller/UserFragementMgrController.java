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
import me.qyh.blog.pageparam.UserFragementQueryParam;
import me.qyh.blog.service.ConfigService;
import me.qyh.blog.service.SpaceService;
import me.qyh.blog.service.UIService;
import me.qyh.blog.ui.fragement.UserFragement;
import me.qyh.blog.web.controller.form.UserFragementQueryParamValidator;
import me.qyh.blog.web.controller.form.UserFragementValidator;

@Controller
@RequestMapping("mgr/fragement/user")
public class UserFragementMgrController extends BaseMgrController {

	@Autowired
	private UIService uiService;
	@Autowired
	private UserFragementQueryParamValidator userFragementParamValidator;
	@Autowired
	private UserFragementValidator userFragementValidator;
	@Autowired
	private ConfigService configService;
	@Autowired
	private SpaceService spaceService;

	@InitBinder(value = "userFragementQueryParam")
	protected void initUserFragementQueryParamBinder(WebDataBinder binder) {
		binder.setValidator(userFragementParamValidator);
	}

	@InitBinder(value = "userFragement")
	protected void initUserFragementBinder(WebDataBinder binder) {
		binder.setValidator(userFragementValidator);
	}

	@RequestMapping("index")
	public String index(@Validated UserFragementQueryParam userFragementQueryParam, BindingResult result, Model model) {
		if (result.hasErrors()) {
			userFragementQueryParam = new UserFragementQueryParam();
			userFragementQueryParam.setCurrentPage(1);
		}
		userFragementQueryParam.setPageSize(configService.getPageSizeConfig().getUserFragementPageSize());
		model.addAttribute("page", uiService.queryUserFragement(userFragementQueryParam));
		model.addAttribute("spaces", spaceService.querySpace(new SpaceQueryParam()));
		return "mgr/fragement/user/index";
	}

	@RequestMapping("list")
	@ResponseBody
	public JsonResult listJson(@Validated UserFragementQueryParam userFragementQueryParam, BindingResult result,
			Model model) {
		if (result.hasErrors()) {
			userFragementQueryParam = new UserFragementQueryParam();
			userFragementQueryParam.setCurrentPage(1);
		}
		userFragementQueryParam.setPageSize(configService.getPageSizeConfig().getUserFragementPageSize());
		return new JsonResult(true, uiService.queryUserFragement(userFragementQueryParam));
	}

	@RequestMapping(value = "create", method = RequestMethod.POST)
	@ResponseBody
	public JsonResult create(@RequestBody @Validated final UserFragement userFragement) throws LogicException {
		if (userFragement.isGlobal())
			userFragement.setSpace(null);
		uiService.insertUserFragement(userFragement);
		return new JsonResult(true, new Message("fragement.user.create.success", "创建成功"));
	}

	@RequestMapping(value = "delete", method = RequestMethod.POST)
	@ResponseBody
	public JsonResult delete(@RequestParam("id") Integer id) throws LogicException {
		uiService.deleteUserFragement(id);
		return new JsonResult(true, new Message("fragement.user.delete.success", "删除成功"));
	}

	@RequestMapping(value = "update", method = RequestMethod.POST)
	@ResponseBody
	public JsonResult update(@RequestBody @Validated final UserFragement userFragement) throws LogicException {
		if (userFragement.isGlobal())
			userFragement.setSpace(null);
		uiService.updateUserFragement(userFragement);
		return new JsonResult(true, new Message("fragement.user.update.success", "更新成功"));
	}

	@RequestMapping(value = "get/{id}")
	@ResponseBody
	public UserFragement get(@PathVariable("id") Integer id) throws LogicException {
		return uiService.queryUserFragement(id);
	}
}
