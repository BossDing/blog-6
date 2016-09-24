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
import me.qyh.blog.pageparam.UserWidgetQueryParam;
import me.qyh.blog.service.ConfigService;
import me.qyh.blog.service.UIService;
import me.qyh.blog.ui.widget.UserWidget;
import me.qyh.blog.web.controller.form.UserWidgetQueryParamValidator;
import me.qyh.blog.web.controller.form.UserWidgetValidator;

@Controller
@RequestMapping("mgr/widget/user")
public class UserWidgetMgrController extends BaseMgrController {

	@Autowired
	private UIService uiService;
	@Autowired
	private UserWidgetQueryParamValidator userWidgetParamValidator;
	@Autowired
	private UserWidgetValidator userWidgetValidator;
	@Autowired
	private ConfigService configService;

	@InitBinder(value = "userWidgetQueryParam")
	protected void initUserWidgetQueryParamBinder(WebDataBinder binder) {
		binder.setValidator(userWidgetParamValidator);
	}

	@InitBinder(value = "userWidget")
	protected void initUserWidgetBinder(WebDataBinder binder) {
		binder.setValidator(userWidgetValidator);
	}

	@RequestMapping("index")
	public String index(@Validated UserWidgetQueryParam userWidgetQueryParam, BindingResult result, Model model) {
		if (result.hasErrors()) {
			userWidgetQueryParam = new UserWidgetQueryParam();
			userWidgetQueryParam.setCurrentPage(1);
		}
		userWidgetQueryParam.setPageSize(configService.getPageSizeConfig().getUserWidgetPageSize());
		model.addAttribute("page", uiService.queryUserWidget(userWidgetQueryParam));
		return "mgr/widget/user/index";
	}

	@RequestMapping("list")
	@ResponseBody
	public JsonResult listJson(@Validated UserWidgetQueryParam userWidgetQueryParam, BindingResult result,
			Model model) {
		if (result.hasErrors()) {
			userWidgetQueryParam = new UserWidgetQueryParam();
			userWidgetQueryParam.setCurrentPage(1);
		}
		userWidgetQueryParam.setPageSize(configService.getPageSizeConfig().getUserWidgetPageSize());
		return new JsonResult(true, uiService.queryUserWidget(userWidgetQueryParam));
	}

	@RequestMapping(value = "create", method = RequestMethod.POST)
	@ResponseBody
	public JsonResult create(@RequestBody @Validated final UserWidget userWidget) throws LogicException {
		uiService.insertUserWidget(userWidget);
		return new JsonResult(true, new Message("widget.user.create.success", "创建成功"));
	}

	@RequestMapping(value = "delete", method = RequestMethod.POST)
	@ResponseBody
	public JsonResult delete(@RequestParam("id") Integer id) throws LogicException {
		uiService.deleteUserWidget(id);
		return new JsonResult(true, new Message("widget.user.delete.success", "删除成功"));
	}

	@RequestMapping(value = "update", method = RequestMethod.POST)
	@ResponseBody
	public JsonResult update(@RequestBody @Validated final UserWidget userWidget) throws LogicException {
		uiService.updateUserWidget(userWidget);
		return new JsonResult(true, new Message("widget.user.update.success", "更新成功"));
	}

	@RequestMapping(value = "get/{id}")
	@ResponseBody
	public UserWidget get(@PathVariable("id") Integer id) throws LogicException {
		return uiService.queryUserWidget(id);
	}
}
