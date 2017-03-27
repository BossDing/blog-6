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
package me.qyh.blog.core.thymeleaf.data;

import me.qyh.blog.core.config.UserConfig;
import me.qyh.blog.core.entity.User;
import me.qyh.blog.core.exception.LogicException;

public class UserDataTagProcessor extends DataTagProcessor<User> {

	public UserDataTagProcessor(String name, String dataName) {
		super(name, dataName);
	}

	@Override
	protected User buildPreviewData(DataTagProcessor<User>.Attributes attributes) {
		return getUser();
	}

	@Override
	protected User query(DataTagProcessor<User>.Attributes attributes) throws LogicException {
		return getUser();
	}

	private User getUser() {
		User user = new User(UserConfig.get());
		user.setPassword(null);
		return user;
	}

}
