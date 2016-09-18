package me.qyh.blog.web.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import me.qyh.blog.bean.JsonResult;
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.message.Message;
import me.qyh.blog.pageparam.OauthUserQueryParam;
import me.qyh.blog.service.ConfigService;
import me.qyh.blog.service.OauthService;
import me.qyh.blog.web.controller.form.OauthUserQueryParamValidator;

@Controller
@RequestMapping("mgr/oauth")
public class OauthMgrController extends BaseMgrController {

	@Autowired
	private OauthService oauthService;
	@Autowired
	private ConfigService configService;

	@Autowired
	private OauthUserQueryParamValidator oauthQueryParamValidator;

	@InitBinder(value = "oauthUserQueryParam")
	protected void initBinder(WebDataBinder binder) {
		binder.setValidator(oauthQueryParamValidator);
	}

	@RequestMapping(value = "index")
	public String index(@Validated OauthUserQueryParam oauthUserQueryParam, BindingResult result, ModelMap modelMap)
			throws LogicException {
		if (result.hasErrors()) {
			oauthUserQueryParam = new OauthUserQueryParam();
			oauthUserQueryParam.setCurrentPage(1);
		}
		oauthUserQueryParam.setPageSize(configService.getPageSizeConfig().getOauthUserPageSize());
		modelMap.addAttribute("page", oauthService.queryOauthUsers(oauthUserQueryParam));
		return "mgr/oauth/index";
	}

	@RequestMapping(value = "disable", method = RequestMethod.POST)
	@ResponseBody
	public JsonResult diable(@RequestParam("id") Integer id) throws LogicException {
		oauthService.disableUser(id);
		return new JsonResult(true, new Message("oauth.disable.success", "禁用成功"));
	}
	
	@RequestMapping(value = "enable", method = RequestMethod.POST)
	@ResponseBody
	public JsonResult enable(@RequestParam("id") Integer id) throws LogicException {
		oauthService.enableUser(id);
		return new JsonResult(true, new Message("oauth.enable.success", "解禁成功"));
	}

}