package me.qyh.blog.web.controller.form;

import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import me.qyh.blog.pageparam.UserFragementQueryParam;
import me.qyh.util.Validators;

@Component
public class UserFragementQueryParamValidator implements Validator {

	private static final int MAX_NAME_LENGTH = 5;

	@Override
	public boolean supports(Class<?> clazz) {
		return UserFragementQueryParam.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		UserFragementQueryParam param = (UserFragementQueryParam) target;
		if (param.getCurrentPage() < 1) {
			param.setCurrentPage(1);
		}
		String name = param.getName();
		if (!Validators.isEmptyOrNull(name, true) && param.getName().length() > MAX_NAME_LENGTH) {
			param.setName(param.getName().substring(0, MAX_NAME_LENGTH));
		}
	}
}
