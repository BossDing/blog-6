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
package me.qyh.blog.ui.fragment;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.core.io.Resource;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.CharStreams;

import me.qyh.blog.config.Constants;
import me.qyh.blog.exception.SystemException;
import me.qyh.blog.util.Validators;
import me.qyh.blog.web.controller.form.UserFragmentValidator;

public class FragmentsFactoryBean implements FactoryBean<List<Fragment>> {

	private Map<String, Resource> tplMap = Maps.newHashMap();

	@Override
	public List<Fragment> getObject() throws Exception {
		List<Fragment> fragments = Lists.newArrayList();
		for (Map.Entry<String, Resource> it : tplMap.entrySet()) {
			String tpl = getTpl(it.getValue());
			if (tpl.length() > UserFragmentValidator.MAX_TPL_LENGTH) {
				throw new SystemException("模板片段长度不能超过" + UserFragmentValidator.MAX_TPL_LENGTH + "个字符");
			}
			if (Validators.isEmptyOrNull(tpl, true)) {
				throw new SystemException("模板片段不能为空");
			}
			Fragment fragment = new Fragment();
			fragment.setName(it.getKey());
			fragment.setTpl(tpl);
			fragments.add(fragment);
		}
		return fragments;
	}

	private String getTpl(Resource resource) throws IOException {
		String tpl = null;
		try (InputStream is = resource.getInputStream();
				InputStreamReader ir = new InputStreamReader(is, Constants.CHARSET)) {
			tpl = CharStreams.toString(ir);
		}
		return tpl;
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

}
