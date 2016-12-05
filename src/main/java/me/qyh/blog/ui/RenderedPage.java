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

import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import me.qyh.blog.ui.data.DataBind;
import me.qyh.blog.ui.fragment.Fragment;
import me.qyh.blog.ui.page.Page;

public class RenderedPage {
	private Page page;
	private List<DataBind<?>> binds = Lists.newArrayList();
	private Map<String, Fragment> fragmentMap = Maps.newLinkedHashMap();

	public RenderedPage(Page page, List<DataBind<?>> binds, Map<String, Fragment> fragmentMap) {
		this.page = page;
		this.binds = binds;
		this.fragmentMap = fragmentMap;
	}

	public Map<String, Object> getDatas() {
		Map<String, Object> map = Maps.newHashMap();
		for (DataBind<?> bind : binds) {
			map.put(bind.getDataName(), bind.getData());
		}
		return map;
	}

	public Page getPage() {
		return page;
	}

	public void setPage(Page page) {
		this.page = page;
	}

	public Map<String, Fragment> getFragmentMap() {
		return fragmentMap;
	}

	public String getTemplateName() {
		return page.getTemplateName();
	}
}
