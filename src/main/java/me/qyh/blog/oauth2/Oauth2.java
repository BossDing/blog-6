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
package me.qyh.blog.oauth2;

/**
 * 
 * @author Administrator
 *
 */
public interface Oauth2 {

	/**
	 * 获取授权路径
	 * 
	 * @param state
	 *            随机码
	 * @return 授权路径
	 */
	String getAuthorizeUrl(String state);

	/**
	 * 服务id
	 * 
	 * @return 服务id
	 */
	String getId();

	/**
	 * 服务名称
	 * 
	 * @return 服务名
	 */
	String getName();

	/**
	 * 通过凭证查询用户信息
	 * 
	 * @param code
	 *            凭证
	 * @return 用户信息
	 */
	UserInfo getUserInfo(String code);

}
