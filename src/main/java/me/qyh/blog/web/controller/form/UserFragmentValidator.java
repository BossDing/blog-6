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

import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;

import me.qyh.blog.core.ui.fragment.UserFragment;

@Component
public class UserFragmentValidator extends FragmentValidator {

	private static final int MAX_DESCRIPTION_LENGTH = 500;

	@Override
	public boolean supports(Class<?> clazz) {
		return UserFragment.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		super.validate(target, errors);
		if (errors.hasErrors()) {
			return;
		}
		UserFragment userFragment = (UserFragment) target;
		String description = userFragment.getDescription();
		if (description == null) {
			errors.reject("fragment.user.description.null", "模板片段描述不能为空");
			return;
		}
		if (description.length() > MAX_DESCRIPTION_LENGTH) {
			errors.reject("fragment.user.description.toolong", new Object[] { MAX_DESCRIPTION_LENGTH },
					"模板片段描述长度不能超过" + MAX_DESCRIPTION_LENGTH + "个字符");
			return;
		}
	}
}
