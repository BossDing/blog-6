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
package me.qyh.blog.bean;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import me.qyh.blog.message.Message;
import me.qyh.blog.message.MessageSerializer;

/**
 * 导入失败信息
 * 
 * @author Administrator
 *
 */
public class ImportError implements Comparable<ImportError> {

	private int index;
	@JsonSerialize(using = MessageSerializer.class)
	private Message message;

	/**
	 * default
	 */
	public ImportError() {
		super();
	}

	/**
	 * 构造器
	 * 
	 * @param index
	 *            序号
	 * @param message
	 *            失败信息
	 */
	public ImportError(int index, Message message) {
		this.index = index;
		this.message = message;
	}

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	public Message getMessage() {
		return message;
	}

	public void setMessage(Message message) {
		this.message = message;
	}

	@Override
	public int compareTo(ImportError o) {
		return index < o.index ? 1 : (index == o.index) ? 0 : -1;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + index;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ImportError other = (ImportError) obj;
		if (index != other.index)
			return false;
		return true;
	}

}
