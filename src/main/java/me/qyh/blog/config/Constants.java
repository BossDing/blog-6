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

import me.qyh.blog.message.Message;

/**
 * 系统常量
 * 
 * @author Administrator
 *
 */
public class Constants {

	/**
	 * session储存管理员的key
	 */
	public static final String USER_SESSION_KEY = "user";
	/**
	 * session储存验证码的key
	 */
	public static final String VALIDATE_CODE_SESSION_KEY = "captchaInSession";
	/**
	 * 默认编码
	 */
	public static final Charset CHARSET = StandardCharsets.UTF_8;
	/**
	 * session中储存最后一次认证失败的地址的key
	 */
	public static final String LAST_AUTHENCATION_FAIL_URL = "lastAuthencationFailUrl";
	/**
	 * session中储存预览模板内容的key
	 */
	public static final String TEMPLATE_PREVIEW_KEY = "templatePreview";
	/**
	 * 系统异常消息
	 */
	public static final Message SYSTEM_ERROR = new Message("error.system", "系统异常");
	/**
	 * 系统默认分页条数
	 */
	public static final int DEFAULT_PAGE_SIZE = 10;
	/**
	 * 默认文章分页地址
	 */
	public static final String DEFAULT_ARTICLE_LIST_PATH = "article/list";
	/**
	 * 默认文章详情访问地址
	 */
	public static final String DEFAULT_ARTICLE_DETAIL_PATH = "space/{alias}/article/{idOrAlias}";

	private Constants() {

	}

}
