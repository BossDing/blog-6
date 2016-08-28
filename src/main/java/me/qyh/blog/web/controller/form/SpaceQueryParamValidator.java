package me.qyh.blog.web.controller.form;

import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import me.qyh.blog.pageparam.SpaceQueryParam;

@Component
public class SpaceQueryParamValidator implements Validator {

	private static final int MAX_NAME_LENGTH = 5;
	private static final int MAX_ALIAS_LENGTH = 5;

	@Override
	public boolean supports(Class<?> clazz) {
		return SpaceQueryParam.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		SpaceQueryParam param = (SpaceQueryParam) target;
		String name = param.getName();
		if (name != null && name.length() > MAX_NAME_LENGTH) {
			param.setName(name.substring(0, MAX_NAME_LENGTH));
		}
		String alias = param.getAlias();
		if (alias != null && alias.length() > MAX_ALIAS_LENGTH) {
			param.setAlias(alias.substring(0, MAX_ALIAS_LENGTH));
		}
	}

}
