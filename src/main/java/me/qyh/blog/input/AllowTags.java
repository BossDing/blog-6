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
package me.qyh.blog.input;

import java.util.ArrayList;
import java.util.List;

import me.qyh.util.Validators;

/**
 * 
 * @author Administrator
 *
 */
public class AllowTags {

	private String simpleTags;
	private List<Tag> tags = new ArrayList<>();

	public void setSimpleTags(String simpleTags) {
		this.simpleTags = simpleTags;
	}

	public List<Tag> getTags() {
		if (!Validators.isEmptyOrNull(simpleTags, true)) {
			String[] names = this.simpleTags.split(",");
			for (String name : names) {
				if (name.isEmpty()) {
					continue;
				}
				Tag tag = new Tag();
				tag.setName(name);
				tags.add(tag);
			}
		}
		return tags;
	}

	public void setTags(List<Tag> tags) {
		this.tags = tags;
	}

	/**
	 * 添加允许的标签
	 * 
	 * @param tag
	 */
	public void addTag(Tag tag) {
		this.tags.add(tag);
	}

}
