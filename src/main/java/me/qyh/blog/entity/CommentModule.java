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
package me.qyh.blog.entity;

import java.io.Serializable;
import java.util.Objects;

import me.qyh.blog.util.Validators;

/**
 * 评论区域
 * 
 * @author Administrator
 *
 */
public class CommentModule implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * 评论类型
	 * 
	 * @author Administrator
	 *
	 */
	public enum ModuleType {
		ARTICLE, USERPAGE;
	}

	private ModuleType type;// 评论类型
	private Integer id;// 关联id

	public CommentModule(ModuleType type, Integer id) {
		super();
		this.type = type;
		this.id = id;
	}

	public CommentModule() {
		super();
	}

	public ModuleType getType() {
		return type;
	}

	public void setType(ModuleType type) {
		this.type = type;
	}

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@Override
	public int hashCode() {
		return Objects.hash(type, id);
	}

	@Override
	public boolean equals(Object obj) {
		if (Validators.baseEquals(this, obj)) {
			CommentModule rhs = (CommentModule) obj;
			return Objects.equals(this.type, rhs.type) && Objects.equals(this.id, rhs.id);
		}
		return false;
	}

}
