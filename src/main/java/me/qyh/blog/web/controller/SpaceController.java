package me.qyh.blog.web.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import me.qyh.blog.exception.LogicException;
import me.qyh.blog.service.UIService;
import me.qyh.blog.ui.Params;
import me.qyh.blog.ui.Template;
import me.qyh.blog.ui.page.SysPage.PageTarget;
import me.qyh.blog.web.interceptor.SpaceContext;

@Controller
@RequestMapping("space/{alias}")
public class SpaceController extends BaseController {

	@Autowired
	private UIService uiService;

	@RequestMapping(value = { "/", "" })
	public Template index() throws LogicException {
		return uiService.renderSysPage(SpaceContext.get(), PageTarget.INDEX, new Params());
	}

	@RequestMapping("page/{idOrAlias}")
	public Template userPage(@PathVariable("idOrAlias") String idOrAlias) throws LogicException {
		return uiService.renderUserPage(idOrAlias);
	}

}
