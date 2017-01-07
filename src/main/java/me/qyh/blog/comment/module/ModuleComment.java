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
package me.qyh.blog.comment.module;

import java.util.Objects;

import com.google.gson.annotations.Expose;

import me.qyh.blog.comment.base.BaseComment;
import me.qyh.blog.util.Validators;

/**
 * 一种类似页面性质的评论，
 * 
 * @author Administrator
 *
 */
public class ModuleComment extends BaseComment<ModuleComment> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	@Expose(serialize = false, deserialize = true)
	private CommentModule module;

	public CommentModule getModule() {
		return module;
	}

	public void setModule(CommentModule module) {
		this.module = module;
	}

	@Override
	public boolean matchParent(ModuleComment parent) {
		return super.matchParent(parent) && this.module.equals(parent.module);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.id);
	}

	@Override
	public boolean equals(Object obj) {
		if (Validators.baseEquals(this, obj)) {
			ModuleComment rhs = (ModuleComment) obj;
			return Objects.equals(this.id, rhs.id);
		}
		return false;
	}
}
