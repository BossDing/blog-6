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

import com.google.common.collect.Lists;

import me.qyh.blog.ui.fragment.Fragment;
import me.qyh.blog.ui.page.Page;

public class ExportPage {

	private Page page;
	private List<Fragment> fragments = Lists.newArrayList();

	public Page getPage() {
		return page;
	}

	public void setPage(Page page) {
		this.page = page.toExportPage();
	}

	public List<Fragment> getFragments() {
		return fragments;
	}

	public void setFragments(List<Fragment> fragments) {
		for (Fragment fragment : fragments) {
			this.fragments.add(fragment.toExportFragment());
		}
	}

	public ExportPage(Page page, List<Fragment> fragments) {
		setPage(page);
		setFragments(fragments);
	}

	public ExportPage() {
	}

}
