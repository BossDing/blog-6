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

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * 系统常量
 * 
 * @author Administrator
 *
 */
public class Constants {

	public static final String USER_SESSION_KEY = "user";
	public static final String VALIDATE_CODE_SESSION_KEY = "captchaInSession";
	public static final Charset CHARSET = StandardCharsets.UTF_8;
	public static final String LAST_AUTHENCATION_FAIL_URL = "lastAuthencationFailUrl";
	public static final String TEMPLATE_PREVIEW_KEY = "templatePreview";

	private Constants() {

	}

}
