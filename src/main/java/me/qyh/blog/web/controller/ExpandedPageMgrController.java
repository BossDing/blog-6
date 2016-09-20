package me.qyh.blog.web.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import me.qyh.blog.bean.JsonResult;
import me.qyh.blog.config.Constants;
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.message.Message;
import me.qyh.blog.service.UIService;
import me.qyh.blog.ui.TplRender;
import me.qyh.blog.ui.TplRenderException;
import me.qyh.blog.ui.page.ExpandedPage;
import me.qyh.blog.web.controller.form.PageValidator;

@RequestMapping("mgr/page/expanded")
@Controller
public class ExpandedPageMgrController extends BaseMgrController {

	@Autowired
	private PageValidator pageValidator;

	@InitBinder(value = "expandedPage")
	protected void initBinder(WebDataBinder binder) {
		binder.setValidator(pageValidator);
	}

	@Autowired
	private UIService uiService;
	@Autowired
	private TplRender tplRender;

	@RequestMapping("index")
	public String index(Model model) {
		model.addAttribute("pages", uiService.queryExpandedPage());
		return "mgr/page/expanded/index";
	}

	@RequestMapping(value = "delete", method = RequestMethod.POST)
	@ResponseBody
	public JsonResult revert(@RequestParam("id") Integer id) throws LogicException {
		uiService.deleteExpandedPage(id);
		return new JsonResult(true, new Message("page.expanded.delete.success", "还原成功"));
	}

	@RequestMapping(value = "build", method = RequestMethod.POST)
	@ResponseBody
	public JsonResult build(@RequestBody @Validated ExpandedPage expandedPage, HttpServletRequest request,
			HttpServletResponse response) throws LogicException {
		ExpandedPage copy = new ExpandedPage();
		BeanUtils.copyProperties(expandedPage, copy);
		uiService.renderPreviewPage(expandedPage);
		try {
			tplRender.tryRender(expandedPage, request, response);
		} catch (TplRenderException e) {
			return new JsonResult(false, e.getRenderErrorDescription());
		}
		uiService.buildTpl(copy);
		return new JsonResult(true, new Message("page.expanded.build.success", "保存成功"));
	}

	@RequestMapping(value = "build", method = RequestMethod.GET)
	public String build(@RequestParam("id") Integer id, Model model, RedirectAttributes ra) {
		try {
			model.addAttribute("page", uiService.queryExpandedPage(id));
			return "mgr/page/expanded/build";
		} catch (LogicException e) {
			ra.addFlashAttribute(ERROR, e.getLogicMessage());
			return "redirect:/mgr/page/expanded/index";
		}
	}

	@RequestMapping(value = "parseWidget", method = RequestMethod.POST)
	@ResponseBody
	public JsonResult parseWidget(@RequestBody @Validated ExpandedPage page) throws LogicException {
		return new JsonResult(true, uiService.parseWidget(page));
	}

	@RequestMapping(value = "preview", method = RequestMethod.POST)
	@ResponseBody
	public JsonResult preview(@RequestBody @Validated ExpandedPage expandedPage, HttpServletRequest request,
			HttpServletResponse response) throws LogicException {
		try {
			uiService.renderPreviewPage(expandedPage);
			String rendered = tplRender.tryRender(expandedPage, request, response);
			request.getSession().setAttribute(Constants.TEMPLATE_PREVIEW_KEY, rendered);
			return new JsonResult(true, rendered);
		} catch (TplRenderException e) {
			return new JsonResult(false, e.getRenderErrorDescription());
		}
	}

}
