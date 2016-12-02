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
package me.qyh.blog.entity;

import java.sql.Timestamp;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * 
 * @author Administrator
 *
 */
public class Space extends BaseLockResource {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * 
	 */
	private String name;// 空间名
	private String alias;
	private Timestamp createDate;// 创建时间
	private Boolean isPrivate;

	private Boolean articleHidden;
	private Boolean isDefault;
	private Integer articlePageSize;

	/**
	 * default
	 */
	public Space() {
		super();
	}

	/**
	 * 
	 * @param id
	 *            空间id
	 */
	public Space(Integer id) {
		super(id);
	}

	public String getAlias() {
		return alias;
	}

	public void setAlias(String alias) {
		this.alias = alias;
	}

	public Timestamp getCreateDate() {
		return createDate;
	}

	public void setCreateDate(Timestamp createDate) {
		this.createDate = createDate;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String getResourceId() {
		return "Space-" + alias;
	}

	public Boolean getIsPrivate() {
		return isPrivate;
	}

	public void setIsPrivate(Boolean isPrivate) {
		this.isPrivate = isPrivate;
	}

	public Boolean getArticleHidden() {
		return articleHidden;
	}

	public void setArticleHidden(Boolean articleHidden) {
		this.articleHidden = articleHidden;
	}

	public Boolean getIsDefault() {
		return isDefault;
	}

	public void setIsDefault(Boolean isDefault) {
		this.isDefault = isDefault;
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder().append(id).build();
	}

	public Integer getArticlePageSize() {
		return articlePageSize;
	}

	public void setArticlePageSize(Integer articlePageSize) {
		this.articlePageSize = articlePageSize;
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
		Space rhs = (Space) obj;
		return new EqualsBuilder().append(id, rhs.id).isEquals();
	}

}
