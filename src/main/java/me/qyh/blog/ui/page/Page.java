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

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import me.qyh.blog.entity.BaseEntity;
import me.qyh.blog.entity.Space;
import me.qyh.blog.exception.SystemException;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Page extends BaseEntity implements Cloneable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	protected static final String PREFIX = "Page:";

	private Space space;// 所属空间
	private String tpl;// 组织片段，用来将片段组织在一起
	private PageType type;

	public enum PageType {
		SYSTEM, USER, EXPANDED, ERROR, LOCK
	}

	public Space getSpace() {
		return space;
	}

	public void setSpace(Space space) {
		this.space = space;
	}

	public void setTpl(String tpl) {
		this.tpl = tpl;
	}

	public String getTpl() {
		return tpl;
	}

	public PageType getType() {
		return type;
	}

	public void setType(PageType type) {
		this.type = type;
	}

	@JsonIgnore
	public String getTemplateName() {
		return PREFIX + getId() + "-" + getType();
	}

	public Page() {
		super();
	}

	public Page(Integer id) {
		super(id);
	}

	public Page(Space space) {
		this.space = space;
	}

	public static boolean isTpl(String templateName) {
		return templateName.startsWith(PREFIX);
	}

	public Page toExportPage() {
		Page page = new Page();
		page.setTpl(tpl);
		page.setType(type);
		return page;
	}

	@Override
	public Object clone() {
		try {
			return super.clone();
		} catch (CloneNotSupportedException e) {
			throw new SystemException(e.getMessage(), e);
		}
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder().append(id).build();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}
		if (obj == this) {
			return true;
		}
		if (obj.getClass() != getClass()) {
			return false;
		}
		Page rhs = (Page) obj;
		return new EqualsBuilder().append(id, rhs.id).isEquals();
	}
}
