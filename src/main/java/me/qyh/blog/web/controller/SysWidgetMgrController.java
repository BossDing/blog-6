package me.qyh.blog.web.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import me.qyh.blog.bean.JsonResult;
import me.qyh.blog.service.UIService;

@Controller
@RequestMapping("mgr/widget/sys")
public class SysWidgetMgrController extends BaseMgrController {

	@Autowired
	private UIService uiService;

	@RequestMapping("all")
	@ResponseBody
	public JsonResult all() {
		return new JsonResult(true, uiService.querySysWidgets());
	}

}
