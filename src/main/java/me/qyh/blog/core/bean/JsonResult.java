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
package me.qyh.blog.core.bean;

import me.qyh.blog.core.message.Message;

/**
 * 用于Json结果的返回
 * 
 * @author mhlx
 *
 */
public class JsonResult {

	private boolean success;
	private Object data;

	private Message message;

	/**
	 * @param success
	 *            是否成功
	 * @param data
	 *            数据
	 */
	public JsonResult(boolean success, Object data) {
		this.success = success;
		this.data = data;
	}

	/**
	 * 
	 * @param success
	 *            是否成功
	 * @param message
	 *            信息
	 */
	public JsonResult(boolean success, Message message) {
		this.success = success;
		this.message = message;
	}

	/**
	 * 
	 * @param success
	 *            是否成功
	 */
	public JsonResult(boolean success) {
		this.success = success;
	}

	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public Object getData() {
		return data;
	}

	public void setData(Object data) {
		this.data = data;
	}

	public Message getMessage() {
		return message;
	}

	public void setMessage(Message message) {
		this.message = message;
	}

	@Override
	public String toString() {
		return "JsonResult [success=" + success + ", data=" + data + ", message=" + message + "]";
	}

}
