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

import java.util.List;

import com.google.common.collect.Lists;

import me.qyh.blog.message.Message;

/**
 * 
 * @author Administrator
 *
 */
public class ImportSuccess {

	private int index;
	private List<Message> warnings = Lists.newArrayList();

	/**
	 * default
	 */
	public ImportSuccess() {
		super();
	}

	/**
	 * 
	 * @param index
	 *            序号
	 */
	public ImportSuccess(int index) {
		this.index = index;
	}

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	public List<Message> getWarnings() {
		return warnings;
	}

	public void setWarning(List<Message> warnings) {
		this.warnings = warnings;
	}

	/**
	 * 添加警告信息
	 * 
	 * @param warning
	 */
	public void addWarning(Message warning) {
		this.warnings.add(warning);
	}

}
