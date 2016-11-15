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
package me.qyh.blog.file;

import java.io.Serializable;

/**
 * 缩略图链接
 * 
 * @author Administrator
 *
 */
public class ThumbnailUrl implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String small;
	private String middle;
	private String large;

	/**
	 * @param small
	 *            小尺寸缩略图链接
	 * @param middle
	 *            中尺寸缩略图链接
	 * @param large
	 *            大尺寸缩略图链接
	 */
	public ThumbnailUrl(String small, String middle, String large) {
		super();
		this.small = small;
		this.middle = middle;
		this.large = large;
	}

	public String getSmall() {
		return small;
	}

	public String getMiddle() {
		return middle;
	}

	public String getLarge() {
		return large;
	}

}
