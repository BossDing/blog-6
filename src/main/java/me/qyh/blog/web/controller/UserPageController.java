package me.qyh.blog.web.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import me.qyh.blog.exception.LogicException;
import me.qyh.blog.service.UIService;
import me.qyh.blog.ui.Template;

@Controller
@RequestMapping("page")
public class UserPageController {

	@Autowired
	private UIService uiService;

	@RequestMapping("{id}")
	public Template index(@PathVariable Integer id) throws LogicException {
		return uiService.renderUserPage(id);
	}

}
