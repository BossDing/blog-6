package me.qyh.blog.web.controller.form;

import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import me.qyh.blog.entity.BlogFile;
import me.qyh.util.Validators;

@Component
public class BlogFileValidator implements Validator {

	private static final int MAX_NAME_LENGTH = 20;

	@Override
	public boolean supports(Class<?> clazz) {
		return BlogFile.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		BlogFile file = (BlogFile) target;
		String name = file.getName();
		if (Validators.isEmptyOrNull(name, true)) {
			errors.reject("file.name.blank", "文件名称为空");
			return;
		}
		if (name.length() > MAX_NAME_LENGTH) {
			errors.reject("file.name.overlength", new Object[] { MAX_NAME_LENGTH },
					"文件名称不能超过" + MAX_NAME_LENGTH + "个字符");
			return;
		}
	}

}
