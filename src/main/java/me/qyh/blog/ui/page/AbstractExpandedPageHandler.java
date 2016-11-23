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
package me.qyh.blog.ui.page;

import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.springframework.core.io.Resource;

import me.qyh.blog.config.Constants;
import me.qyh.blog.exception.SystemException;

public abstract class AbstractExpandedPageHandler implements ExpandedPageHandler {

	private String _template;
	private int id;
	private String name;

	public AbstractExpandedPageHandler(int id, String name, Resource template) {
		try (InputStream is = template.getInputStream()) {
			_template = IOUtils.toString(is, Constants.CHARSET);
		} catch (Exception e) {
			throw new SystemException(e.getMessage(), e);
		}
		this.id = id;
		this.name = name;
	}

	@Override
	public final String getTemplate() {
		return _template;
	}

	@Override
	public int id() {
		return id;
	}

	@Override
	public String name() {
		return name;
	}

}
