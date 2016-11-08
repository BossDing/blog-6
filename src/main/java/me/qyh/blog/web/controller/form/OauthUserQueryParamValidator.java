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
import org.springframework.validation.Validator;

import me.qyh.blog.pageparam.OauthUserQueryParam;

@Component
public class OauthUserQueryParamValidator implements Validator {

	private static final int MAX_NICKNAME_LENGTH = 20;

	@Override
	public boolean supports(Class<?> clazz) {
		return OauthUserQueryParam.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		OauthUserQueryParam param = (OauthUserQueryParam) target;
		String nickname = param.getNickname();
		if (nickname != null && nickname.length() > MAX_NICKNAME_LENGTH) {
			param.setNickname(nickname.substring(0, MAX_NICKNAME_LENGTH));
		}
		if (param.getCurrentPage() < 1) {
			param.setCurrentPage(1);
		}
	}

}
