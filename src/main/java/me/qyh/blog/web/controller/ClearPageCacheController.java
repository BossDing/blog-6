package me.qyh.blog.web.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.thymeleaf.TemplateEngine;

import me.qyh.blog.bean.JsonResult;
import me.qyh.blog.message.Message;

@RequestMapping("mgr")
public class ClearPageCacheController extends BaseMgrController {

	@Autowired
	private TemplateEngine templateEngine;

	public JsonResult clearPageCache() {
		templateEngine.getConfiguration().getTemplateManager().clearCaches();
		return new JsonResult(true, new Message("clearPageCache.success"));
	}

}
