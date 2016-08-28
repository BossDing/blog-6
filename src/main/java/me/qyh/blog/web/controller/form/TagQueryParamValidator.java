package me.qyh.blog.web.controller.form;

import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import me.qyh.blog.pageparam.TagQueryParam;

@Component
public class TagQueryParamValidator implements Validator {

	private static final int MAX_TAG_LENGTH = 20;

	@Override
	public boolean supports(Class<?> clazz) {
		return TagQueryParam.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		TagQueryParam param = (TagQueryParam) target;
		if (param.getCurrentPage() < 1) {
			param.setCurrentPage(1);
		}
		String tag = param.getTag();
		if (tag != null && tag.length() > MAX_TAG_LENGTH) {
			param.setTag(tag.substring(0, MAX_TAG_LENGTH));
		}
	}

}
