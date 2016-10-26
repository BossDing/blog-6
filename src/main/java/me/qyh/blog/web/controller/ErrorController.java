package me.qyh.blog.web.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import me.qyh.blog.service.UIService;
import me.qyh.blog.ui.RenderedPage;
import me.qyh.blog.ui.UIContext;
import me.qyh.blog.ui.page.ErrorPage.ErrorCode;

@Controller
@RequestMapping("error")
public class ErrorController {

	private static final Logger logger = LoggerFactory.getLogger(ErrorController.class);

	@Autowired
	private UIService uiService;

	@RequestMapping("200")
	public String handler200() {
		return handlerError(200);
	}

	@RequestMapping("400")
	public String handler400() {
		return handlerError(400);
	}

	@RequestMapping("403")
	public String handler403() {
		return handlerError(403);
	}

	@RequestMapping("404")
	public String handler404() {
		return handlerError(404);
	}

	@RequestMapping("405")
	public String handler405() {
		return handlerError(405);
	}

	@RequestMapping("500")
	public String handler500() {
		return handlerError(500);
	}

	@RequestMapping("ui")
	public String handlerUI() {
		return "error/ui";
	}

	private String handlerError(int error) {
		try {
			RenderedPage page = uiService.renderErrorPage(null, ErrorCode.valueOf("ERROR_" + error));
			UIContext.set(page);
			return page.getTemplateName();
		} catch (Throwable e) {
			logger.error("渲染错误码" + error + "时发生异常:" + e.getMessage(), e);
			return "error/" + error;
		}
	}

}
