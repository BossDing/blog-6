package me.qyh.blog.web.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import me.qyh.blog.service.UIService;
import me.qyh.blog.ui.UIContext;
import me.qyh.blog.ui.page.ErrorPage.ErrorCode;
import me.qyh.blog.ui.page.Page;
import me.qyh.blog.web.interceptor.SpaceContext;

@Controller
@RequestMapping("space/{alias}/error")
public class SpaceErrorController extends BaseController {

	private static final Logger logger = LoggerFactory.getLogger(SpaceErrorController.class);

	@Autowired
	private UIService uiService;

	@RequestMapping("200")
	public String handler200(Model model) {
		return handlerError(model, 200);
	}

	@RequestMapping("400")
	public String handler400(Model model) {
		return handlerError(model, 400);
	}

	@RequestMapping("404")
	public String handler404(Model model) {
		return handlerError(model, 404);
	}

	@RequestMapping("403")
	public String handler403(Model model) {
		return handlerError(model, 403);
	}

	@RequestMapping("405")
	public String handler405(Model model) {
		return handlerError(model, 405);
	}

	@RequestMapping("500")
	public String handler500(Model model) {
		return handlerError(model, 500);
	}

	private String handlerError(Model model, int error) {
		try {
			Page page = uiService.renderErrorPage(SpaceContext.get(), ErrorCode.valueOf("ERROR_" + error));
			UIContext.set(page);
			return page.getTemplateName();
		} catch (Throwable e) {
			logger.error("空间" + SpaceContext.get() + "渲染错误码" + error + "时发生异常:" + e.getMessage(), e);
			return "error/" + error;
		}
	}
}
