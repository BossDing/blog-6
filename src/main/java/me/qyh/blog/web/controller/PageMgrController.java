package me.qyh.blog.web.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import me.qyh.blog.config.Constants;

@Controller
@RequestMapping("mgr/page")
public class PageMgrController extends BaseMgrController {

	@RequestMapping(value = "preview", method = RequestMethod.GET)
	public String preview(HttpServletRequest request, Model model) {
		HttpSession session = request.getSession(false);
		if (session == null) {
			return "redirect:/mgr/tpl/build/index";
		} else {
			String rendered = (String) session.getAttribute(Constants.TEMPLATE_PREVIEW_KEY);
			if (rendered == null) {
				return "redirect:/mgr/tpl/build/index";
			} else {
				model.addAttribute(Constants.TEMPLATE_PREVIEW_KEY, rendered);
				return "mgr/page/preview";
			}
		}
	}
}
