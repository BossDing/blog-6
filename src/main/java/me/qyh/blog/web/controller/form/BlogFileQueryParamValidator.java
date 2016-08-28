package me.qyh.blog.web.controller.form;

import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import me.qyh.blog.pageparam.BlogFileQueryParam;

@Component
public class BlogFileQueryParamValidator implements Validator {

	@Override
	public boolean supports(Class<?> clazz) {
		return BlogFileQueryParam.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		BlogFileQueryParam param = (BlogFileQueryParam) target;
		if (param.getCurrentPage() < 1) {
			param.setCurrentPage(1);
		}
	}

}
