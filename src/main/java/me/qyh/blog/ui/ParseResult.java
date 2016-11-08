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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import me.qyh.blog.ui.data.DataBind;
import me.qyh.blog.ui.fragment.Fragment;

public class ParseResult {

	private List<DataBind<?>> binds = new ArrayList<DataBind<?>>();
	private Map<String, Fragment> fragments = new HashMap<String, Fragment>();
	private Set<DataTag> unkownDatas = new LinkedHashSet<DataTag>();
	private Set<String> unkownFragments = new LinkedHashSet<String>();

	public List<DataBind<?>> getBinds() {
		return binds;
	}

	public void setBinds(List<DataBind<?>> binds) {
		this.binds = binds;
	}

	public Set<DataTag> getUnkownDatas() {
		return unkownDatas;
	}

	public Set<String> getUnkownFragments() {
		return unkownFragments;
	}

	public void addUnkownData(DataTag tag) {
		unkownDatas.add(tag);
	}

	public void addUnkownFragment(String name) {
		unkownFragments.add(name);
	}

	public Map<String, Fragment> getFragments() {
		return fragments;
	}

	public void putFragment(String key, Fragment v) {
		fragments.put(key, v);
	}

}
