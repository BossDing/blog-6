package me.qyh.blog.web.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
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
import me.qyh.blog.pageparam.SpaceQueryParam;
import me.qyh.blog.pageparam.UserPageQueryParam;
import me.qyh.blog.service.ConfigService;
import me.qyh.blog.service.SpaceService;
import me.qyh.blog.service.UIService;
import me.qyh.blog.ui.RenderedPage;
import me.qyh.blog.ui.TplRender;
import me.qyh.blog.ui.TplRenderException;
import me.qyh.blog.ui.page.UserPage;
import me.qyh.blog.web.controller.form.PageValidator;
import me.qyh.blog.web.controller.form.UserPageQueryParamValidator;

@Controller
@RequestMapping("mgr/page/user")
public class UserPageMgrController extends BaseMgrController {

	@Autowired
	private UserPageQueryParamValidator userPageParamValidator;
	@Autowired
	private UIService uiService;
	@Autowired
	private SpaceService spaceService;
	@Autowired
	private ConfigService configService;
	@Autowired
	private TplRender tplRender;

	@Autowired
	private PageValidator pageValidator;

	@InitBinder(value = "userPage")
	protected void initBinder(WebDataBinder binder) {
		binder.setValidator(pageValidator);
	}

	@InitBinder(value = "userPageQueryParam")
	protected void initUserPageQueryParamBinder(WebDataBinder binder) {
		binder.setValidator(userPageParamValidator);
	}

	@RequestMapping("index")
	public String index(@Validated UserPageQueryParam param, BindingResult result, Model Model) {
		if (result.hasErrors()) {
			param = new UserPageQueryParam();
			param.setCurrentPage(1);
		}
		param.setPageSize(configService.getPageSizeConfig().getUserPagePageSize());
		Model.addAttribute("page", uiService.queryUserPage(param));
		return "mgr/page/user/index";
	}

	@RequestMapping(value = "build", method = RequestMethod.POST)
	@ResponseBody
	public JsonResult build(@RequestBody @Validated UserPage userPage, HttpServletRequest request,
			HttpServletResponse response) throws LogicException {
		RenderedPage page = uiService.renderPreviewPage(userPage);
		try {
			tplRender.tryRender(page, request, response);
		} catch (TplRenderException e) {
			return new JsonResult(false, e.getRenderErrorDescription());
		}
		uiService.buildTpl(userPage);
		return new JsonResult(true, new Message("page.user.build.success", "保存成功"));
	}

	@RequestMapping(value = "new")
	public String build(Model model) {
		model.addAttribute("page", new UserPage());
		SpaceQueryParam param = new SpaceQueryParam();
		model.addAttribute("spaces", spaceService.querySpace(param));
		return "mgr/page/user/build";
	}

	@RequestMapping(value = "update")
	public String update(@RequestParam("id") Integer id, Model model, RedirectAttributes ra) {
		UserPage page = uiService.queryUserPage(id);
		if (page == null) {
			ra.addFlashAttribute(ERROR, new Message("page.user.notExists", "自定义页面不存在"));
			return "redirect:/mgr/page/user/index";
		}
		model.addAttribute("page", page);
		SpaceQueryParam param = new SpaceQueryParam();
		model.addAttribute("spaces", spaceService.querySpace(param));
		return "mgr/page/user/build";
	}

	@RequestMapping(value = "preview", method = RequestMethod.POST)
	@ResponseBody
	public JsonResult preview(@RequestBody @Validated UserPage userPage, HttpServletRequest request,
			HttpServletResponse response) throws LogicException {
		try {
			RenderedPage page = uiService.renderPreviewPage(userPage);
			String rendered = tplRender.tryRender(page, request, response);
			request.getSession().setAttribute(Constants.TEMPLATE_PREVIEW_KEY, rendered);
			return new JsonResult(true, rendered);
		} catch (TplRenderException e) {
			return new JsonResult(false, e.getRenderErrorDescription());
		}
	}

	@RequestMapping(value = "delete", method = RequestMethod.POST)
	@ResponseBody
	public JsonResult delete(@RequestParam("id") Integer id) throws LogicException {
		uiService.deleteUserPage(id);
		return new JsonResult(true, new Message("page.user.delete.success", "删除成功"));
	}

}
