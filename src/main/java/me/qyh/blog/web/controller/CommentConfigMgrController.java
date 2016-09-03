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
import me.qyh.blog.config.CommentConfig;
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.message.Message;
import me.qyh.blog.service.ConfigService;
import me.qyh.blog.web.controller.form.CommentConfigValidator;

@RequestMapping("mgr/config/comment")
@Controller
public class CommentConfigMgrController extends BaseMgrController {

	@Autowired
	private ConfigService configService;
	@Autowired
	private CommentConfigValidator commentConfigValidator;

	@InitBinder(value = "commentConfig")
	protected void initBinder(WebDataBinder binder) {
		binder.setValidator(commentConfigValidator);
	}

	@RequestMapping("index")
	public String index(Model model) {
		model.addAttribute("commentConfig", configService.getCommentConfig());
		return "mgr/config/comment";
	}

	@RequestMapping(value = "update", method = RequestMethod.POST)
	@ResponseBody
	public JsonResult update(@Validated @RequestBody CommentConfig commentConfig) throws LogicException {
		configService.updateCommentConfig(commentConfig);
		return new JsonResult(true, new Message("commentConfig.update.success", "更新成功"));
	}

}
