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

/**
 * 
 * @author Administrator
 *
 */
public interface OauthService {

	/**
	 * 插入|更新 账号
	 * 
	 * @param user
	 *            oauth用户信息
	 * @return 用户
	 */
	OauthUser insertOrUpdate(OauthUser user);

	/**
	 * 查询所有的绑定账号
	 * 
	 * @return 已绑定账号的集合
	 */
	List<OauthBind> queryAllBind();

	/**
	 * 查询绑定账号
	 * 
	 * @param user
	 *            oauth用户
	 * @return 绑定信息，如果没有绑定，返回null
	 * @throws LogicException
	 *             查询过程中发生逻辑异常
	 */
	OauthBind queryBind(OauthUser user) throws LogicException;

	/**
	 * 绑定用户
	 * 
	 * @param oauthUser
	 *            待绑定的用户
	 * @throws LogicException
	 *             绑定过程中发生逻辑异常
	 */
	void bind(OauthUser oauthUser) throws LogicException;

	/**
	 * 解除绑定
	 * 
	 * @param id
	 *            绑定纪录的id
	 * @throws LogicException
	 *             解除过程中发生逻辑异常
	 */
	void unbind(Integer id) throws LogicException;

	/**
	 * 分页查询用户
	 * 
	 * @param param
	 *            查询参数
	 * @return oauth用户列表
	 */
	PageResult<OauthUser> queryOauthUsers(OauthUserQueryParam param);

	/**
	 * 禁用用户
	 * 
	 * @param id
	 *            用户id
	 * @throws LogicException
	 *             禁用过程中发生的逻辑异常
	 */
	void disableUser(Integer id) throws LogicException;

	/**
	 * 解除禁用
	 * 
	 * @param id
	 *            用户id
	 * @throws LogicException
	 *             解禁过程中发生的逻辑异常
	 */
	void enableUser(Integer id) throws LogicException;

}
