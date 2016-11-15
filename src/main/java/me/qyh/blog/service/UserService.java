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
package me.qyh.blog.service;

import me.qyh.blog.entity.User;
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.web.controller.form.LoginBean;

/**
 * 
 * @author Administrator
 *
 */
public interface UserService {

	/**
	 * 更新用户
	 * 
	 * @param user
	 *            待更新的用户
	 * @throws LogicException
	 */
	void updateUser(User user) throws LogicException;

	/**
	 * 登录
	 * 
	 * @param loginBean
	 *            登录信息
	 * @return 登录成功后的用户
	 * @throws LogicException
	 */
	User login(LoginBean loginBean) throws LogicException;

	/**
	 * 查询管理员
	 * 
	 * @return 管理员
	 */
	User select();

}
