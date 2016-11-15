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
package me.qyh.blog.metaweblog;

import me.qyh.blog.message.Message;

/**
 * xmlrpc 错误
 * 
 * @author Administrator
 *
 */
public final class FaultException extends Exception {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final String code;
	private final Message desc;

	/**
	 * @param code
	 *            错误码
	 * @param dest
	 *            错误描述
	 */
	public FaultException(String code, Message dest) {
		super();
		this.code = code;
		this.desc = dest;
	}

	public String getCode() {
		return code;
	}

	public Message getDesc() {
		return desc;
	}

}