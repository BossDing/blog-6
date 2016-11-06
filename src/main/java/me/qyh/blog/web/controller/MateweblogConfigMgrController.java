package me.qyh.blog.web.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import me.qyh.blog.bean.JsonResult;
import me.qyh.blog.config.UploadConfig;
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.message.Message;
import me.qyh.blog.service.ConfigService;
import me.qyh.blog.web.controller.form.UploadConfigValidator;

@RequestMapping("mgr/config/metaweblogConfig")
@Controller
public class MateweblogConfigMgrController extends BaseMgrController {

	@Autowired
	private ConfigService configService;
	@Autowired
	private UploadConfigValidator uploadConfigValidator;

	@InitBinder(value = "uploadConfig")
	protected void initBinder(WebDataBinder binder) {
		binder.setValidator(uploadConfigValidator);
	}

	@RequestMapping("index")
	public String index(Model model) {
		model.addAttribute("config", configService.getMetaweblogConfig());
		return "mgr/config/metaweblogConfig";
	}

	@RequestMapping(value = "update", method = RequestMethod.POST)
	@ResponseBody
	public JsonResult update(@Validated @RequestBody UploadConfig uploadConfig) throws LogicException {
		configService.updateMetaweblogConfig(uploadConfig);
		return new JsonResult(true, new Message("metaweblogConfig.update.success", "更新成功"));
	}

}
