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
package me.qyh.blog.bean;

import me.qyh.blog.file.FileServer;

/**
 * 文件服务描述
 * 
 * @author Administrator
 *
 */
public class FileServerBean {

	private int id;
	private String name;

	/**
	 * default
	 */
	public FileServerBean() {
		super();
	}

	/**
	 * 文件服务描述构造器
	 * 
	 * @param server
	 *            文件服务
	 */
	public FileServerBean(FileServer server) {
		this.id = server.id();
		this.name = server.name();
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
