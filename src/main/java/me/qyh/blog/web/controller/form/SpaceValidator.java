package me.qyh.blog.web.controller.form;

import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import me.qyh.blog.entity.Space;
import me.qyh.util.Validators;

@Component
public class SpaceValidator implements Validator {

	private static final int MAX_NAME_LENGTH = 20;
	private static final int MAX_ALIAS_LENGTH = 20;

	@Override
	public boolean supports(Class<?> clazz) {
		return Space.class.equals(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		Space space = (Space) target;
		String name = space.getName();
		if (Validators.isEmptyOrNull(name, true)) {
			errors.reject("space.name.blank", "空间名为空");
			return;
		}
		if (name.length() > MAX_NAME_LENGTH) {
			errors.reject("space.name.toolong", new Object[] { MAX_NAME_LENGTH }, "空间名不能超过" + MAX_NAME_LENGTH + "个字符");
			return;
		}

		String alias = space.getAlias();
		if (Validators.isEmptyOrNull(alias, true)) {
			errors.reject("space.alias.blank", "别名为空");
			return;
		}
		if (alias.length() > MAX_ALIAS_LENGTH) {
			errors.reject("space.alias.toolong", new Object[] { MAX_ALIAS_LENGTH }, "别名不能超过" + MAX_NAME_LENGTH + "个字符");
			return;
		}
		char[] chars = alias.toCharArray();
		for (char ch : chars) {
			if (!isAllowLetter(ch) && ch != '-') {
				errors.reject("space.alias.invalid", "别名只能包含英文字母，数字和'-'");
				return;
			}
		}
	}

	/**
	 * Character.isLetterOrDigit()无法判断中文
	 * 
	 * @param ch
	 * @return
	 * @see Character#isLetterOrDigit(char)
	 */
	private boolean isAllowLetter(char ch) {
		return ('a' <= ch && ch <= 'z') || ('A' <= ch && ch <= 'Z') || ('0' <= ch && ch <= '9') || ('-' == ch)
				|| ('_' == ch);
	}

}
