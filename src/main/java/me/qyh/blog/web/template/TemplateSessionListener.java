package me.qyh.blog.web.template;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import org.springframework.web.context.WebApplicationContext;

import me.qyh.blog.core.config.Constants;
import me.qyh.blog.core.exception.SystemException;

@Component
public class TemplateSessionListener implements HttpSessionListener, ApplicationContextAware {

	@Autowired
	private TemplateService templateService;

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		if (applicationContext instanceof WebApplicationContext) {
			((WebApplicationContext) applicationContext).getServletContext().addListener(this);
		} else {
			throw new SystemException("必须处于WebApplicationContext中");
		}
	}

	@Override
	public void sessionCreated(HttpSessionEvent se) {
		//
	}

	@Override
	public void sessionDestroyed(HttpSessionEvent se) {
		HttpSession old = se.getSession();
		if (old != null && old.getAttribute(Constants.USER_SESSION_KEY) != null) {
			// 清除预览模板
			templateService.clearPreview();
		}

	}
}