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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.util.CollectionUtils;

import me.qyh.blog.exception.SystemException;

/**
 * oauth2服务提供器
 * 
 * @author Administrator
 *
 */
public class Oauth2Provider {

	private Map<String, Oauth2> oauth2Map = new HashMap<>();

	/**
	 * 根据id查找对应的oauth2服务商
	 * 
	 * @param id
	 *            oauth2服务商id
	 * @return 如果不存在返回null
	 */
	public Oauth2 getOauth2(String id) {
		return oauth2Map.get(id);
	}

	public void setOauth2s(List<Oauth2> oauth2s) {
		if (CollectionUtils.isEmpty(oauth2s)) {
			throw new SystemException("oauth2服务不能为空");
		}
		for (Oauth2 oauth2 : oauth2s) {
			String id = oauth2.getId();
			if (oauth2Map.containsKey(id)) {
				throw new SystemException("oauth服务" + id + "重复");
			}
			oauth2Map.put(id, oauth2);
		}
	}

}
