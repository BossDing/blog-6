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
package me.qyh.blog.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import me.qyh.blog.dao.UserDao;
import me.qyh.blog.entity.User;
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.security.BCrypts;
import me.qyh.blog.service.UserService;
import me.qyh.blog.web.controller.form.LoginBean;
import me.qyh.util.Validators;

@Service
@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
public class UserServiceImpl implements UserService {

	@Autowired
	private UserDao userDao;

	@Override
	public void updateUser(User user) throws LogicException {
		if (!Validators.isEmptyOrNull(user.getPassword(), true)) {
			user.setPassword(BCrypts.encode(user.getPassword()));
		}
		userDao.update(user);
	}

	@Override
	@Transactional(readOnly = true)
	public User login(LoginBean loginBean) throws LogicException {
		User user = userDao.select();
		if (user != null) {
			if (user.getName().equals(loginBean.getUsername())) {
				String encrptPwd = user.getPassword();
				if (BCrypts.matches(loginBean.getPassword(), encrptPwd)) {
					return user;
				}
			}
		}
		throw new LogicException("user.loginFail", "登录失败");
	}

	@Override
	@Transactional(readOnly = true)
	public User select() {
		return userDao.select();
	}
}
