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
package me.qyh.blog.comment;

import me.qyh.blog.config.UserConfig;
import me.qyh.blog.entity.User;
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.util.Validators;

public abstract class CommentUserChecker {

	/**
	 * 检查邮箱是否被允许
	 * 
	 * @param nickame
	 *            昵称
	 * @param email
	 *            邮箱
	 * @throws LogicException
	 *             检查未通过
	 */
	public final void doCheck(final String name, final String email) throws LogicException {
		User user = UserConfig.get();
		String emailOrAdmin = user.getEmail();
		if (!Validators.isEmptyOrNull(emailOrAdmin, true) && emailOrAdmin.equals(email))
			throw new LogicException("comment.email.invalid", "邮件不被允许");
		if (user.getName().equalsIgnoreCase(name))
			throw new LogicException("comment.nickname.invalid", "昵称不被允许");
		checkMore(name, email);
	}

	protected abstract void checkMore(final String name, final String email) throws LogicException;
}