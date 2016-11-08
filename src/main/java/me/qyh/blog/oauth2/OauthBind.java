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

import java.sql.Timestamp;

import me.qyh.blog.entity.Id;

public class OauthBind extends Id {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private OauthUser user;
	private Timestamp bindDate;

	public OauthUser getUser() {
		return user;
	}

	public void setUser(OauthUser user) {
		this.user = user;
	}

	public Timestamp getBindDate() {
		return bindDate;
	}

	public void setBindDate(Timestamp bindDate) {
		this.bindDate = bindDate;
	}

}
