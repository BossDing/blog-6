package me.qyh.blog.web.controller.form;

import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import me.qyh.blog.ui.fragement.UserFragement;
import me.qyh.util.Validators;

@Component
public class UserFragementValidator implements Validator {

	private static final int MAX_NAME_LENGTH = 20;
	private static final int MAX_DESCRIPTION_LENGTH = 500;
	public static final int MAX_TPL_LENGTH = 20000;

	@Override
	public boolean supports(Class<?> clazz) {
		return UserFragement.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		UserFragement userFragement = (UserFragement) target;
		String name = userFragement.getName();
		if (Validators.isEmptyOrNull(name, true)) {
			errors.reject("fragement.user.name.blank", "模板片段名为空");
			return;
		}
		if (name.length() > MAX_NAME_LENGTH) {
			errors.reject("fragement.user.name.toolong", new Object[] { MAX_NAME_LENGTH },
					"模板片段名长度不能超过" + MAX_NAME_LENGTH + "个字符");
			return;
		}
		String description = userFragement.getDescription();
		if (description == null) {
			errors.reject("fragement.user.description.null", "模板片段描述不能为空");
			return;
		}
		if (description != null && description.length() > MAX_DESCRIPTION_LENGTH) {
			errors.reject("fragement.user.description.toolong", new Object[] { MAX_DESCRIPTION_LENGTH },
					"模板片段描述长度不能超过" + MAX_DESCRIPTION_LENGTH + "个字符");
			return;
		}
		String tpl = userFragement.getTpl();
		if (Validators.isEmptyOrNull(tpl, true)) {
			errors.reject("fragement.user.tpl.null", "模板片段模板不能为空");
			return;
		}
		if (tpl != null && tpl.length() > MAX_TPL_LENGTH) {
			errors.reject("fragement.user.tpl.toolong", new Object[] { MAX_TPL_LENGTH },
					"模板片段模板长度不能超过" + MAX_TPL_LENGTH + "个字符");
			return;
		}
	}

}
