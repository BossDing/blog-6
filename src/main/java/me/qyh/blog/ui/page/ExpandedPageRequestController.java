package me.qyh.blog.ui.page;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

import me.qyh.blog.service.UIService;
import me.qyh.blog.ui.Params;
import me.qyh.blog.ui.UIContext;

public class ExpandedPageRequestController implements Controller {

	@Autowired
	private UIService uiService;
	@Autowired
	private ExpandedPageServer expandedPageServer;

	@Override
	public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
		ExpandedPageHandler handler = expandedPageServer.getPageHandler(request);
		Params params = handler.fromHttpRequest(request);
		if (params == null) {
			params = new Params();
		}
		Page page = uiService.renderExpandedPage(handler.id(), params);
		UIContext.set(page);
		return new ModelAndView(page.getTemplateName());
	}

}
