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

import me.qyh.blog.entity.Comment;
import me.qyh.util.Validators;

@Component
public class CommentBeanValidator implements Validator {

	public static final int MAX_COMMENT_LENGTH = 500;

	@Override
	public boolean supports(Class<?> clazz) {
		return CommentBean.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		CommentBean cb = (CommentBean) target;
		Comment comment = cb.getComment();
		if (comment == null) {
			errors.reject("comment.blank", "评论不能为空");
			return;
		}
		String content = comment.getContent();
		if (Validators.isEmptyOrNull(content, true)) {
			errors.reject("comment.content.blank", "回复内容不能为空");
			return;
		}
		if (content.length() > MAX_COMMENT_LENGTH) {
			errors.reject("comment.content.toolong", new Object[] { MAX_COMMENT_LENGTH },
					"回复的内容不能超过" + MAX_COMMENT_LENGTH + "个字符");
			return;
		}
		String email = cb.getEmail();
		if (email != null) {
			email = email.trim();
			if (!email.isEmpty()) {
				if (email.length() > UserValidator.MAX_EMAIL_LENGTH) {
					errors.reject("comment.email.toolong", new Object[] { UserValidator.MAX_EMAIL_LENGTH },
							"邮箱不能超过" + UserValidator.MAX_EMAIL_LENGTH + "位");
					return;
				}
				if (!UserValidator.EMAIL_PATTERN.matcher(email).matches()) {
					errors.reject("comment.email.invalid", "邮箱不是正确的格式");
					return;
				}
			} else {
				email = null;
			}
		}
		cb.setEmail(email);
	}

}
