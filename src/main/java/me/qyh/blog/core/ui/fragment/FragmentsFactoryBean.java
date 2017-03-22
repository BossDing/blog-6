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
package me.qyh.blog.core.ui.fragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.core.io.Resource;
import org.springframework.util.CollectionUtils;

import me.qyh.blog.core.exception.SystemException;
import me.qyh.blog.util.Resources;
import me.qyh.blog.util.Validators;
import me.qyh.blog.web.controller.form.UserFragmentValidator;

public class FragmentsFactoryBean implements FactoryBean<List<Fragment>> {

	private Map<String, Resource> tplMap = new HashMap<>();
	private List<Fragment> fragments = new ArrayList<>();

	@Override
	public List<Fragment> getObject() throws Exception {
		List<Fragment> fragments = new ArrayList<>();
		for (Map.Entry<String, Resource> it : tplMap.entrySet()) {
			Fragment fragment = new Fragment();
			fragment.setName(it.getKey());
			fragment.setTpl(Resources.readResourceToString(it.getValue()));
			doValid(fragment);
			fragments.add(fragment);
		}
		if (!CollectionUtils.isEmpty(this.fragments)) {
			for (Fragment fragment : this.fragments) {
				checkExists(fragments, fragment.getName());
				doValid(fragment);
				fragments.add(fragment);
			}
		}
		return fragments;
	}

	private void checkExists(List<Fragment> fragments, String name) {
		fragments.stream().filter(fr -> fr.getName().equals(name)).findAny().ifPresent(fr -> {
			throw new SystemException(name + "已经存在");
		});
	}

	private void doValid(Fragment fragment) {
		String name = fragment.getName();
		if (Validators.isEmptyOrNull(name, true)) {
			throw new SystemException("模板片段名称不能为空");
		}
		if (!name.matches(UserFragmentValidator.NAME_PATTERN)) {
			throw new SystemException("模板片段名称只能为数字或者中英文");
		}
		String tpl = fragment.getTpl();
		if (tpl.length() > UserFragmentValidator.MAX_TPL_LENGTH) {
			throw new SystemException("模板片段长度不能超过" + UserFragmentValidator.MAX_TPL_LENGTH + "个字符");
		}
		if (Validators.isEmptyOrNull(tpl, true)) {
			throw new SystemException("模板片段不能为空");
		}
	}

	@Override
	public Class<?> getObjectType() {
		return List.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	public void setTplMap(Map<String, Resource> tplMap) {
		this.tplMap = tplMap;
	}

	public void setFragments(List<Fragment> fragments) {
		this.fragments = fragments;
	}

}
