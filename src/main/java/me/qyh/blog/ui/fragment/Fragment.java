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

import java.io.Serializable;
import java.util.Objects;

import me.qyh.blog.ui.data.DataTagProcessor;
import me.qyh.blog.util.Validators;

/**
 * 片段，用来展现数据
 * 
 * @see DataTagProcessor
 * @author Administrator
 *
 */
public class Fragment implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * 片段名，全局唯一
	 */
	private String name;
	private String tpl;

	public String getTpl() {
		return tpl;
	}

	public void setTpl(String tpl) {
		this.tpl = tpl;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.name);
	}

	@Override
	public boolean equals(Object obj) {
		if (Validators.baseEquals(this, obj)) {
			Fragment other = (Fragment) obj;
			return Objects.equals(this.name, other.name);
		}
		return false;
	}

	public final Fragment toExportFragment() {
		Fragment f = new Fragment();
		f.setName(name);
		f.setTpl(tpl);
		return f;
	}

}
