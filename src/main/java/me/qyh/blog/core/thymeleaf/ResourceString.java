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
package me.qyh.blog.core.thymeleaf;

import java.io.IOException;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.core.io.Resource;

import me.qyh.blog.util.Resources;

/**
 * 将Resource转化为String
 * 
 * @author mhlx
 *
 */
public class ResourceString implements FactoryBean<String> {

	private final String str;

	public ResourceString(Resource resource) throws IOException {
		this.str = Resources.readResourceToString(resource);
	}

	@Override
	public String getObject() throws Exception {
		return str;
	}

	@Override
	public Class<?> getObjectType() {
		return String.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
