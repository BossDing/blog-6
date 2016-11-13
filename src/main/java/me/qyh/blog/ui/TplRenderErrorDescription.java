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
package me.qyh.blog.ui;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import me.qyh.blog.message.Message;
import me.qyh.blog.message.MessageSerializer;

public class TplRenderErrorDescription implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private Integer line;// 行号
	private Integer col;// 列号
	private ArrayList<String> templateNames = new ArrayList<String>();// 模板名
	private String expression;// 表达式
	@JsonSerialize(using = MessageSerializer.class)
	private Message message;// 错误信息

	public TplRenderErrorDescription() {
	}

	public void addTemplateName(String templateName) {
		templateNames.add(templateName);
	}

	public List<String> getTemplateNames() {
		return templateNames;
	}

	public String getExpression() {
		return expression;
	}

	public void setLine(int line) {
		this.line = line;
	}

	public void setExpression(String expression) {
		this.expression = expression;
	}

	public Integer getLine() {
		return line;
	}

	public void setLine(Integer line) {
		this.line = line;
	}

	public Integer getCol() {
		return col;
	}

	public void setCol(Integer col) {
		this.col = col;
	}

	public Message getMessage() {
		return message;
	}

	public void setMessage(Message message) {
		this.message = message;
	}

	public String getTemplateName() {
		if (!CollectionUtils.isEmpty(templateNames)) {
			StringBuilder sb = new StringBuilder();
			for (String templateName : templateNames) {
				sb.append(templateName).append("->");
			}
			sb.delete(sb.length() - 2, sb.length());
			return sb.toString();
		}
		return null;
	}

}
