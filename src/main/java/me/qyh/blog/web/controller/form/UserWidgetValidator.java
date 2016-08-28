package me.qyh.blog.web.controller.form;

import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import me.qyh.blog.ui.widget.UserWidget;
import me.qyh.util.Validators;

@Component
public class UserWidgetValidator implements Validator {

	private static final int MAX_NAME_LENGTH = 20;
	private static final int MAX_DESCRIPTION_LENGTH = 500;
	public static final int MAX_TPL_LENGTH = 20000;

	@Override
	public boolean supports(Class<?> clazz) {
		return UserWidget.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		UserWidget userWidget = (UserWidget) target;
		String name = userWidget.getName();
		if (Validators.isEmptyOrNull(name, true)) {
			errors.reject("widget.user.name.blank", "挂件名为空");
			return;
		}
		if (name.length() > MAX_NAME_LENGTH) {
			errors.reject("widget.user.name.toolong", new Object[] { MAX_NAME_LENGTH },
					"挂件名长度不能超过" + MAX_NAME_LENGTH + "个字符");
			return;
		}
		String description = userWidget.getDescription();
		if (description == null) {
			errors.reject("widget.user.description.null", "挂件描述不能为空");
			return;
		}
		if (description != null && description.length() > MAX_DESCRIPTION_LENGTH) {
			errors.reject("widget.user.description.toolong", new Object[] { MAX_DESCRIPTION_LENGTH },
					"挂件描述长度不能超过" + MAX_DESCRIPTION_LENGTH + "个字符");
			return;
		}
		String tpl = userWidget.getDefaultTpl();
		if (tpl == null) {
			errors.reject("widget.user.tpl.null", "挂件模板不能为空");
			return;
		}
		if (tpl != null && tpl.length() > MAX_TPL_LENGTH) {
			errors.reject("widget.user.tpl.toolong", new Object[] { MAX_TPL_LENGTH },
					"挂件模板长度不能超过" + MAX_TPL_LENGTH + "个字符");
			return;
		}
	}

}
