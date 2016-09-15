package me.qyh.blog.web.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import me.qyh.blog.exception.LogicException;
import me.qyh.blog.service.UIService;
import me.qyh.blog.ui.Params;
import me.qyh.blog.ui.page.Page;
import me.qyh.blog.ui.page.SysPage.PageTarget;

@Controller
public class IndexController {

	@Autowired
	private UIService uiService;

	@RequestMapping(value = { "/", "" })
	public Page index() throws LogicException {
		return uiService.renderSysPage(null, PageTarget.INDEX, new Params());
	}

}
