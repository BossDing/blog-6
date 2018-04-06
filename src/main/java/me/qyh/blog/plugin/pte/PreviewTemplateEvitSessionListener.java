package me.qyh.blog.plugin.pte;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import me.qyh.blog.core.config.Constants;
import me.qyh.blog.template.service.TemplateService;

public class PreviewTemplateEvitSessionListener implements HttpSessionListener {

	private final TemplateService templateService;

	public PreviewTemplateEvitSessionListener(TemplateService templateService) {
		super();
		this.templateService = templateService;
	}

	@Override
	public void sessionCreated(HttpSessionEvent se) {
		//
	}

	@Override
	public void sessionDestroyed(HttpSessionEvent se) {
		HttpSession session = se.getSession();
		if (session.getAttribute(Constants.USER_SESSION_KEY) != null) {
			templateService.clearPreview();
		}
	}

}
