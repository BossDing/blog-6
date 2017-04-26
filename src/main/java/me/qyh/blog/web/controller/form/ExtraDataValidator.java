package me.qyh.blog.web.controller.form;

import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import me.qyh.blog.core.bean.ExtraData;
import me.qyh.blog.util.Validators;

@Component
public class ExtraDataValidator implements Validator {

	private static final String KEY_PATTERN = "^[A-Za-z0-9_-]+$";
	private static final int MAX_VALUE_LENGTH = 500000;

	@Override
	public boolean supports(Class<?> clazz) {
		return ExtraData.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object o, Errors errors) {
		ExtraData extraData = (ExtraData) o;
		if (Validators.isEmptyOrNull(extraData.getKey(), true)) {
			errors.reject("extraData.key.blank", "key不能为空");
			return;
		}
		if (!extraData.getKey().matches(KEY_PATTERN)) {
			errors.reject("extraData.key.invalid", "key只能为英文字母、数字和_或者-");
			return;
		}
		if (Validators.isEmptyOrNull(extraData.getValue(), true)) {
			errors.reject("extraData.value.blank", "内容不能为空");
			return;
		}
		if (extraData.getValue().length() > MAX_VALUE_LENGTH) {
			errors.reject("extraData.value.toolong", new Object[] { MAX_VALUE_LENGTH },
					"内容不能超过" + MAX_VALUE_LENGTH + "个字符");
			return;
		}
	}

}
