package me.qyh.blog.web.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import me.qyh.blog.bean.JsonResult;
import me.qyh.blog.config.Constants;
import me.qyh.blog.message.Message;
import me.qyh.blog.service.UIService;
import me.qyh.blog.ui.page.Page;
import me.qyh.blog.ui.page.Page.PageType;
import me.qyh.blog.ui.widget.Widget;
import me.qyh.blog.ui.widget.Widget.WidgetType;

@Controller
@RequestMapping("mgr/page")
public class PageMgrController extends BaseMgrController {
	@Autowired
	private UIService uiService;

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

	@ResponseBody
	@RequestMapping(value = "{pageType}/{pageId}/widget/{widgetType}/{widgetId}/delete", method = RequestMethod.POST)
	public JsonResult deleteWidgetTpl(@PathVariable("pageType") PageType pageType,
			@PathVariable("pageId") Integer pageId, @PathVariable("widgetType") WidgetType widgetType,
			@PathVariable("widgetId") Integer widgetId) {
		Page page = new Page();
		page.setId(pageId);
		page.setType(pageType);

		Widget widget = new Widget();
		widget.setType(widgetType);
		widget.setId(widgetId);

		uiService.deleteWidgetTpl(page, widget);

		return new JsonResult(true, new Message("page.widgetTpl.delete.success", "挂件模板还原成功"));

	}
}
