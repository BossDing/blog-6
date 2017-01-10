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
package me.qyh.blog.util;

import java.util.UUID;

import me.qyh.blog.ui.utils.UIUtils;

/**
 * 
 * @author Administrator
 *
 */
@UIUtils(name="uuids")
public class UUIDs {

	/**
	 * private
	 */
	private UUIDs() {
		super();
	}

	/**
	 * 获取uuid字符串
	 * 
	 * @return uuid
	 */
	public static String uuid() {
		return UUID.randomUUID().toString().replace("-", "");
	}

}
