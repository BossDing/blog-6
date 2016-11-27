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
package me.qyh.blog.config;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import me.qyh.blog.entity.User;
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.exception.SystemException;
import me.qyh.blog.security.BCrypts;
import me.qyh.util.Validators;

public final class UserConfig {

	private static final EncodedResource userRes = new EncodedResource(
			new ClassPathResource("resources/user.properties"), Constants.CHARSET);
	private static final String USERNAME = "username";
	private static final String PASSWORD = "password";
	private static final String EMAIL = "email";

	private static Properties pros;
	private static User user;

	static {
		try {
			pros = PropertiesLoaderUtils.loadProperties(userRes);
		} catch (IOException e) {
			throw new SystemException(e.getMessage(), e);
		}
		load();
	}

	public static void update(User user) throws LogicException {
		save(user);
	}

	private synchronized static void save(User user) {
		if (!Validators.isEmptyOrNull(user.getPassword(), true))
			pros.setProperty(PASSWORD, BCrypts.encode(user.getPassword()));
		pros.setProperty(USERNAME, user.getName());
		pros.setProperty(EMAIL, user.getEmail() == null ? "" : user.getEmail());
		try (OutputStream os = FileUtils.openOutputStream(userRes.getResource().getFile())) {
			pros.store(os, "");
		} catch (IOException e) {
			throw new SystemException(e.getMessage(), e);
		}
		load();
	}

	private static void load() {
		String username = pros.getProperty(USERNAME);
		String password = pros.getProperty(PASSWORD);
		String email = pros.getProperty(EMAIL, "");
		if (Validators.isEmptyOrNull(username, true))
			username = "admin";
		if (Validators.isEmptyOrNull(password, true))
			password = BCrypts.encode("admin");
		user = new User();
		user.setEmail(email);
		user.setName(username);
		user.setPassword(password);
	}

	public static User get() {
		return new User(user);
	}
}
