/*
 * Copyright 2016 qyh.me
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package me.qyh.blog.web.controller.form;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import me.qyh.blog.bean.ExportPage;
import me.qyh.blog.ui.fragment.Fragment;
import me.qyh.blog.ui.page.LockPage;
import me.qyh.blog.ui.page.Page;
import me.qyh.blog.ui.page.UserPage;
import me.qyh.blog.util.Validators;

@Component
public class ExportPageValidator implements Validator {

	private FragmentValidator fragmentValidator = new FragmentValidator();
	private ThisPageValidator thisPageValidator = new ThisPageValidator();

	@Override
	public boolean supports(Class<?> clazz) {
		return ExportPage.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		ExportPage exportPage = (ExportPage) target;
		Page page = exportPage.getPage();
		if (page == null) {
			errors.reject("page.null", "页面不能为空");
			return;
		}
		thisPageValidator.validate(page, errors);
		if (errors.hasErrors()) {
			return;
		}
		List<Fragment> fragments = exportPage.getFragments();
		if (fragments == null) {
			exportPage.setFragments(new ArrayList<>());
		} else {
			for (Fragment fragment : fragments) {
				fragmentValidator.validate(fragment, errors);
				if (errors.hasErrors()) {
					return;
				}
			}
		}
	}

	private final class ThisPageValidator extends PageValidator {

		@Override
		public void validate(Object target, Errors errors) {
			Page page = (Page) target;
			String pageTpl = page.getTpl();
			if (Validators.isEmptyOrNull(pageTpl, true)) {
				errors.reject("page.tpl.null", "页面模板不能为空");
				return;
			}

			if (pageTpl.length() > PAGE_TPL_MAX_LENGTH) {
				errors.reject("page.tpl.toolong", new Object[] { PAGE_TPL_MAX_LENGTH },
						"页面模板不能超过" + PAGE_TPL_MAX_LENGTH + "个字符");
				return;
			}

			if (page instanceof LockPage) {
				LockPage lockPage = (LockPage) page;
				if (Validators.isEmptyOrNull(lockPage.getLockType(), true)) {
					errors.reject("page.locktype.empty", "页面锁类型不能为空");
					return;
				}
			}

			if (page instanceof UserPage) {
				UserPage userPage = (UserPage) page;
				String alias = userPage.getAlias();
				if (Validators.isEmptyOrNull(alias, true)) {
					errors.reject("page.alias.blank", "页面别名不能为空");
					return;
				}
				if (alias.length() > PAGE_ALIAS_MAX_LENGTH) {
					errors.reject("page.alias.toolong", new Object[] { PAGE_ALIAS_MAX_LENGTH },
							"页面别名不能超过" + PAGE_ALIAS_MAX_LENGTH + "个字符");
					return;
				}
				if (!validateUserPageAlias(alias)) {
					errors.reject("page.alias.invalid", "页面别名不被允许");
					return;
				}
			}
		}

	}

}
