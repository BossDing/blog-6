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
package me.qyh.blog.dao;

import java.util.List;

import me.qyh.blog.oauth2.OauthBind;
import me.qyh.blog.oauth2.OauthUser;

/**
 * 
 * @author Administrator
 *
 */
public interface OauthBindDao {

	/**
	 * 插入绑定记录
	 * 
	 * @param bind
	 *            待插入的绑定记录
	 */
	void insert(OauthBind bind);

	/**
	 * 根据id删除绑定记录
	 * 
	 * @param id
	 *            纪录id
	 */
	void deleteById(Integer id);

	/**
	 * 查询所有的绑定纪录
	 * 
	 * @return 记录集
	 */
	List<OauthBind> selectAll();

	/**
	 * 根据用户查询绑定记录
	 * 
	 * @param user
	 *            用户
	 * @return 如果没有绑定，返回null
	 */
	OauthBind selectByOauthUser(OauthUser user);

	/**
	 * 根据id查询绑定纪录
	 * 
	 * @param id
	 *            纪录id
	 * @return 如果没有，返回null
	 * 
	 */
	OauthBind selectById(Integer id);

}
