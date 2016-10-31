package me.qyh.blog.ui;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.DispatcherServlet;

public class UIErrorCatchDispatchServlet extends DispatcherServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static final Logger logger = LoggerFactory.getLogger(UIErrorCatchDispatchServlet.class);

	@Override
	protected void doService(HttpServletRequest request, HttpServletResponse response) throws Exception {
		try {
			super.doService(request, response);
		} catch (TplRenderException e) {
			Throwable ori = e.getOriginal();
			logger.error(ori.getMessage(), ori);
			if (!response.isCommitted()) {
				request.setAttribute("description", e.getRenderErrorDescription());
				request.getRequestDispatcher("/error/ui").forward(request, response);
			}
		}
	}

}
