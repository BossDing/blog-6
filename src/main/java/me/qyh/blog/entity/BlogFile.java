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

import me.qyh.blog.file.CommonFile;

/**
 * 
 * @author Administrator
 *
 */
public class BlogFile extends BaseEntity {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private Timestamp createDate;
	private Timestamp lastModifyDate;

	/**
	 * 文件类型
	 * 
	 * @author Administrator
	 *
	 */
	public enum BlogFileType {
		DIRECTORY, // 文件夹
		FILE // 文件
	}

	private BlogFileType type;
	private CommonFile cf; // 实际文件

	private Integer lft;
	private Integer rgt;
	private BlogFile parent; // 父节点
	private String name; // 名称
	private String path;

	public CommonFile getCf() {
		return cf;
	}

	public void setCf(CommonFile cf) {
		this.cf = cf;
	}

	public Integer getLft() {
		return lft;
	}

	public void setLft(Integer lft) {
		this.lft = lft;
	}

	public Integer getRgt() {
		return rgt;
	}

	public void setRgt(Integer rgt) {
		this.rgt = rgt;
	}

	public BlogFile getParent() {
		return parent;
	}

	public void setParent(BlogFile parent) {
		this.parent = parent;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public BlogFileType getType() {
		return type;
	}

	public void setType(BlogFileType type) {
		this.type = type;
	}

	public boolean isDir() {
		return BlogFileType.DIRECTORY.equals(type);
	}

	public boolean isFile() {
		return BlogFileType.FILE.equals(type);
	}

	/**
	 * 是否是根节点
	 * 
	 * @return
	 */
	public boolean isRoot() {
		return parent == null;
	}

	public Timestamp getLastModifyDate() {
		return lastModifyDate == null ? createDate : lastModifyDate;
	}

	public void setLastModifyDate(Timestamp lastModifyDate) {
		this.lastModifyDate = lastModifyDate;
	}

	public Timestamp getCreateDate() {
		return createDate;
	}

	public void setCreateDate(Timestamp createDate) {
		this.createDate = createDate;
	}

	public int getWidth() {
		if (rgt != null && lft != null) {
			return rgt - lft + 1;
		}
		return 0;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}
}
