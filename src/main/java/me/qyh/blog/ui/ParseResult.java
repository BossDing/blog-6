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

import java.util.Set;

import com.google.common.collect.Sets;

public class ParseResult {

	private Set<String> fragments = Sets.newLinkedHashSet();
	private Set<DataTag> dataTags = Sets.newLinkedHashSet();

	public boolean hasFragment() {
		return !fragments.isEmpty();
	}

	public boolean hasDataTag() {
		return !dataTags.isEmpty();
	}

	public Set<DataTag> getDataTags() {
		return dataTags;
	}

	public void setDataTags(Set<DataTag> dataTags) {
		this.dataTags = dataTags;
	}

	public Set<String> getFragments() {
		return fragments;
	}

	public void setFragments(Set<String> fragments) {
		this.fragments = fragments;
	}

	public void addFragment(String fragment) {
		this.fragments.add(fragment);
	}

	public void addDataTag(DataTag tag) {
		this.dataTags.add(tag);
	}
}
