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

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import me.qyh.blog.message.Message;
import me.qyh.blog.message.MessageListSerializer;

public class ImportSuccess {

	private int index;
	@JsonSerialize(using = MessageListSerializer.class)
	private List<Message> warnings = new ArrayList<Message>();

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

	public ImportSuccess() {

	}

	public ImportSuccess(int index) {
		this.index = index;
	}

	public void addWarning(Message warning) {
		this.warnings.add(warning);
	}

}
