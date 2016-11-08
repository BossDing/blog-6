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

import java.util.List;

import me.qyh.blog.exception.LogicException;
import me.qyh.blog.oauth2.OauthBind;
import me.qyh.blog.oauth2.OauthUser;
import me.qyh.blog.pageparam.OauthUserQueryParam;
import me.qyh.blog.pageparam.PageResult;

public interface OauthService {

	/**
	 * 插入|更新 账号
	 * 
	 * @param user
	 */
	void insertOrUpdate(OauthUser user);

	/**
	 * 查询所有的绑定账号
	 * 
	 * @return
	 */
	List<OauthBind> queryAllBind();

	/**
	 * 查询绑定账号
	 * 
	 * @param user
	 * @return
	 * @throws LogicException
	 */
	OauthBind queryBind(OauthUser user) throws LogicException;

	/**
	 * 绑定用户
	 * 
	 * @param oauthUser
	 * @throws LogicException
	 */
	void bind(OauthUser oauthUser) throws LogicException;

	/**
	 * 接触绑定
	 * 
	 * @param id
	 * @throws LogicException
	 */
	void unbind(Integer id) throws LogicException;

	/**
	 * 分页查询用户
	 * 
	 * @param param
	 * @return
	 */
	PageResult<OauthUser> queryOauthUsers(OauthUserQueryParam param);

	/**
	 * 禁用用户
	 * 
	 * @param id
	 * @throws LogicException
	 */
	void disableUser(Integer id) throws LogicException;

	/**
	 * 解除禁用
	 * 
	 * @param id
	 * @throws LogicException
	 */
	void enableUser(Integer id) throws LogicException;

}
