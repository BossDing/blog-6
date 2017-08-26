package me.qyh.blog.web.template;

import java.util.Optional;

import org.springframework.web.method.HandlerMethod;

class TemplateUtils {

	private TemplateUtils() {
		super();
	}

	public static Optional<TemplateController> getTemplateController(Object handler) {
		if (handler instanceof HandlerMethod) {
			Object bean = ((HandlerMethod) handler).getBean();
			if (bean instanceof TemplateController) {
				return Optional.of((TemplateController) bean);
			}
		}
		return Optional.empty();
	}

}
