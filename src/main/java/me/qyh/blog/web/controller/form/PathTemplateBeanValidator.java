package me.qyh.blog.web.controller.form;

import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import me.qyh.blog.util.Validators;
import me.qyh.blog.web.template.PathTemplateBean;

@Component
public class PathTemplateBeanValidator implements Validator {

	@Override
	public boolean supports(Class<?> clazz) {
		return PathTemplateBean.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object o, Errors errors) {
		PathTemplateBean bean = (PathTemplateBean) o;
		String tpl = bean.getTpl();

		if (Validators.isEmptyOrNull(tpl, true)) {
			errors.reject("pathTemplate.tpl.null", "模板不能为空");
			return;
		}

		if (tpl.length() > PageValidator.PAGE_TPL_MAX_LENGTH) {
			errors.reject("pathTemplate.tpl.toolong", new Object[] { PageValidator.PAGE_TPL_MAX_LENGTH },
					"模板不能超过" + PageValidator.PAGE_TPL_MAX_LENGTH + "个字符");
			return;
		}

		PageValidator.validateAlias(bean.getPath(), errors);
		if (errors.hasErrors()) {
			return;
		}

		if (bean.isPub() && bean.isRegistrable()) {
			errors.reject("pathTemplate.type.ambiguous", "无法确定模板类型");
		}
	}

}
