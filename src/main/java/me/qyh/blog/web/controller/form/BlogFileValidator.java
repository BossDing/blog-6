package me.qyh.blog.web.controller.form;

import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import me.qyh.blog.entity.BlogFile;
import me.qyh.util.Validators;

@Component
public class BlogFileValidator implements Validator {

	private static final int MAX_NAME_LENGTH = 20;
	private static final int MAX_PATH_LENGTH = 30;

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
			errors.reject("file.name.toolong", new Object[] { MAX_NAME_LENGTH }, "文件名称不能超过" + MAX_NAME_LENGTH + "个字符");
			return;
		}
		if (!file.isFile()) {
			String path = file.getPath();
			if (Validators.isEmptyOrNull(path, true)) {
				errors.reject("file.path.blank", "文件夹路径不能为空");
				return;
			}
			if (path.length() > MAX_PATH_LENGTH) {
				errors.reject("file.path.toolong", new Object[] { MAX_PATH_LENGTH },
						"文件夹路径不能超过" + MAX_PATH_LENGTH + "个字符");
				return;
			}
			if(!checkPath(path)){
				errors.reject("file.path.valid", "文件夹路径无效");
				return;
			}
		}
	}

	private boolean checkPath(String path) {
		char[] chars = path.toCharArray();
		for (char ch : chars) {
			if (!isAllowLetter(ch))
				return false;
		}
		return true;
	}

	private boolean isAllowLetter(char ch) {
		return ('a' <= ch && ch <= 'z') || ('A' <= ch && ch <= 'Z') || ('0' <= ch && ch <= '9');
	}

}
