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
package me.qyh.blog.lock.support;

import javax.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import me.qyh.blog.lock.LockKey;
import me.qyh.blog.lock.InvalidKeyException;
import me.qyh.blog.lock.ErrorKeyException;
import me.qyh.blog.message.Message;
import me.qyh.blog.security.BCrypts;
import me.qyh.util.Validators;

public class PasswordLock extends SysLock {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static final String PASSWORD_PARAMETER = "password";

	private String password;

	public PasswordLock() {
		super(SysLockType.PASSWORD);
	}

	@Override
	public LockKey getKeyFromRequest(HttpServletRequest request) throws InvalidKeyException {
		final String password = request.getParameter(PASSWORD_PARAMETER);
		if (Validators.isEmptyOrNull(password, true)) {
			throw new InvalidKeyException(new Message("lock.password.password.blank", "密码不能为空"));
		}
		return new LockKey() {

			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			public Object getKey() {
				return password;
			}
		};
	}

	@Override
	public void tryOpen(LockKey key) throws ErrorKeyException {
		if (key != null) {
			Object keyData = key.getKey();
			if (keyData != null && BCrypts.matches(keyData.toString(), password)) {
				return;
			}
		}
		throw new ErrorKeyException(new Message("lock.password.unlock.fail", "密码验证失败"));
	}

	@JsonIgnore
	public String getPassword() {
		return password;
	}

	@JsonProperty
	public void setPassword(String password) {
		this.password = password;
	}

}
