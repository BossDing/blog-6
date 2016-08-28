package me.qyh.blog.ui.page;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.handler.AbstractHandlerMapping;

public class ExpanedPageHandlerMapping extends AbstractHandlerMapping {

	@Autowired
	private ExpandedPageServer expandedPageServer;
	@Autowired
	private ExpandedPageRequestController controller;

	@Override
	protected Object getHandlerInternal(HttpServletRequest request) throws Exception {
		if (!expandedPageServer.isEmpty()) {
			expandedPageServer.getPageHandler(request);
			if (expandedPageServer.getPageHandler(request) != null) {
				return controller;
			}
		}
		return null;
	}

}
