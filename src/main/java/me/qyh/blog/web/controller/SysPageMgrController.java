package me.qyh.blog.web.controller;

import java.util.List;

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
import me.qyh.blog.entity.Space;
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.message.Message;
import me.qyh.blog.pageparam.SpaceQueryParam;
import me.qyh.blog.service.SpaceService;
import me.qyh.blog.service.UIService;
import me.qyh.blog.ui.TplRender;
import me.qyh.blog.ui.TplRenderException;
import me.qyh.blog.ui.page.SysPage;
import me.qyh.blog.ui.page.SysPage.PageTarget;
import me.qyh.blog.web.controller.form.PageValidator;

@RequestMapping("mgr/page/sys")
@Controller
public class SysPageMgrController extends BaseMgrController {

	@Autowired
	private UIService uiService;
	@Autowired
	private SpaceService spaceService;
	@Autowired
	private TplRender tplRender;

	@Autowired
	private PageValidator pageValidator;

	@InitBinder(value = "sysPage")
	protected void initBinder(WebDataBinder binder) {
		binder.setValidator(pageValidator);
	}

	@RequestMapping(value = "index", method = RequestMethod.GET)
	public String index(Model model, RedirectAttributes ra) {
		SpaceQueryParam param = new SpaceQueryParam();
		List<Space> spaces = spaceService.querySpace(param);
		model.addAttribute("spaces", spaces);
		model.addAttribute("pageTargets", PageTarget.values());
		return "mgr/page/sys/index";
	}

	@RequestMapping(value = "build", method = RequestMethod.GET)
	public String build(@RequestParam("target") PageTarget target,
			@RequestParam(required = false, value = "spaceId") Integer spaceId, Model model) {
		model.addAttribute("page", uiService.querySysPage(spaceId == null ? null : new Space(spaceId), target));
		return "mgr/page/sys/build";
	}

	@RequestMapping(value = "build", method = RequestMethod.POST)
	@ResponseBody
	public JsonResult build(@RequestBody @Validated SysPage sysPage, HttpServletRequest request,
			HttpServletResponse response) throws LogicException {
		SysPage copy = new SysPage();
		BeanUtils.copyProperties(sysPage, copy);
		uiService.renderPreviewPage(sysPage);
		try {
			tplRender.tryRender(sysPage, request, response);
		} catch (TplRenderException e) {
			return new JsonResult(false, e.getRenderErrorDescription());
		}
		uiService.buildTpl(copy);
		return new JsonResult(true, new Message("page.sys.build.success", "保存成功"));
	}

	@RequestMapping(value = "parseWidget", method = RequestMethod.POST)
	@ResponseBody
	public JsonResult parseWidget(@RequestBody @Validated SysPage sysPage) throws LogicException {
		return new JsonResult(true, uiService.parseWidget(sysPage));
	}

	@RequestMapping(value = "preview", method = RequestMethod.POST)
	@ResponseBody
	public JsonResult preview(@RequestBody @Validated SysPage sysPage, HttpServletRequest request,
			HttpServletResponse response) throws LogicException {
		try {
			uiService.renderPreviewPage(sysPage);
			String rendered = tplRender.tryRender(sysPage, request, response);
			request.getSession().setAttribute(Constants.TEMPLATE_PREVIEW_KEY, rendered);
			return new JsonResult(true, rendered);
		} catch (TplRenderException e) {
			return new JsonResult(false, e.getRenderErrorDescription());
		}
	}

	@RequestMapping(value = "delete", method = RequestMethod.POST)
	@ResponseBody
	public JsonResult delete(@RequestParam("target") PageTarget target,
			@RequestParam(required = false, value = "spaceId") Integer spaceId) throws LogicException {
		uiService.deleteSysPage(spaceId == null ? null : new Space(spaceId), target);
		return new JsonResult(true, new Message("page.sys.delete.success", "还原成功"));
	}

}
