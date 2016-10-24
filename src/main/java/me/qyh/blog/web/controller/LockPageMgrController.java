package me.qyh.blog.web.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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

import me.qyh.blog.bean.JsonResult;
import me.qyh.blog.config.Constants;
import me.qyh.blog.entity.Space;
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.message.Message;
import me.qyh.blog.service.UIService;
import me.qyh.blog.ui.RenderedPage;
import me.qyh.blog.ui.TplRender;
import me.qyh.blog.ui.TplRenderException;
import me.qyh.blog.ui.page.LockPage;
import me.qyh.blog.web.controller.form.PageValidator;

@RequestMapping("mgr/page/lock")
@Controller
public class LockPageMgrController extends BaseMgrController {

	@Autowired
	private UIService uiService;
	@Autowired
	private TplRender tplRender;

	@Autowired
	private PageValidator pageValidator;

	@InitBinder(value = "lockPage")
	protected void initBinder(WebDataBinder binder) {
		binder.setValidator(pageValidator);
	}

	@RequestMapping(value = "build", method = RequestMethod.GET)
	public String build(@RequestParam("lockType") String lockType,
			@RequestParam(required = false, value = "spaceId") Integer spaceId, Model model) throws LogicException {
		model.addAttribute("page", uiService.queryLockPage(spaceId == null ? null : new Space(spaceId), lockType));
		return "mgr/page/lock/build";
	}

	@RequestMapping(value = "build", method = RequestMethod.POST)
	@ResponseBody
	public JsonResult build(@RequestBody @Validated LockPage lockPage, HttpServletRequest request,
			HttpServletResponse response) throws LogicException {
		RenderedPage page = uiService.renderPreviewPage(lockPage);
		try {
			tplRender.tryRender(page, request, response);
		} catch (TplRenderException e) {
			return new JsonResult(false, e.getRenderErrorDescription());
		}
		uiService.buildTpl(lockPage);
		return new JsonResult(true, new Message("page.lock.build.success", "保存成功"));
	}

	@RequestMapping(value = "preview", method = RequestMethod.POST)
	@ResponseBody
	public JsonResult preview(@RequestBody @Validated LockPage sysPage, HttpServletRequest request,
			HttpServletResponse response) throws LogicException {
		try {
			RenderedPage page = uiService.renderPreviewPage(sysPage);
			String rendered = tplRender.tryRender(page, request, response);
			request.getSession().setAttribute(Constants.TEMPLATE_PREVIEW_KEY, rendered);
			return new JsonResult(true, rendered);
		} catch (TplRenderException e) {
			return new JsonResult(false, e.getRenderErrorDescription());
		}
	}

	@RequestMapping(value = "delete", method = RequestMethod.POST)
	@ResponseBody
	public JsonResult delete(@RequestParam("lockType") String lockType,
			@RequestParam(required = false, value = "spaceId") Integer spaceId) throws LogicException {
		uiService.deleteLockPage(spaceId == null ? null : new Space(spaceId), lockType);
		return new JsonResult(true, new Message("page.lock.delete.success", "还原成功"));
	}

}
