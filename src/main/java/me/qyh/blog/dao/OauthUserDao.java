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

import org.apache.ibatis.annotations.Param;

import me.qyh.blog.oauth2.OauthUser;
import me.qyh.blog.pageparam.OauthUserQueryParam;

/**
 * 
 * @author Administrator
 *
 */
public interface OauthUserDao {

	/**
	 * 根据oauthId和serverId查询对应的用户
	 * 
	 * @param oauthid
	 *            oauth用户的id
	 * @param serverId
	 *            oauth服务id
	 * @return 如果不存在返回null
	 */
	OauthUser selectByOauthIdAndServerId(@Param("oauthid") String oauthid, @Param("serverId") String serverId);

	/**
	 * 插入oauth用户
	 * 
	 * @param user
	 *            待插入的oauth用户
	 */
	void insert(OauthUser user);

	/**
	 * 更新oauth用户
	 * 
	 * @param user
	 *            待更新的oauth用户
	 */
	void update(OauthUser user);

	/**
	 * 查询用户数
	 * 
	 * @param param
	 *            查询参数
	 * @return 用户数
	 */
	int selectCount(OauthUserQueryParam param);

	/**
	 * 查询用户结婚
	 * 
	 * @param param
	 *            查询参数
	 * @return 用户集合
	 */
	List<OauthUser> selectPage(OauthUserQueryParam param);

	/**
	 * 根据id查询用户
	 * 
	 * @param id
	 *            纪录id
	 * @return 如果不存在返回null
	 */
	OauthUser selectById(Integer id);

}
