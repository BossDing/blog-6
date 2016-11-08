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

public class Resize {

	private int width;// 缩略图宽度
	private int height;// 缩略图高度
	private boolean keepRatio = true;// 保持纵横比
	private Integer size;// 如果设置了该属性，其他属性将失效

	public int getWidth() {
		return width;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public int getHeight() {
		return height;
	}

	public Integer getSize() {
		return size;
	}

	public void setHeight(int height) {
		this.height = height;
	}

	public boolean isKeepRatio() {
		return keepRatio;
	}

	public void setKeepRatio(boolean keepRatio) {
		this.keepRatio = keepRatio;
	}

	public void setSize(Integer size) {
		this.size = size;
	}
	
	public Resize(){
		
	}

	public Resize(Integer size) {
		this.size = size;
	}

	public Resize(int width, int height, boolean keepRatio) {
		this.width = width;
		this.height = height;
		this.keepRatio = keepRatio;
	}

	public Resize(int width, int height) {
		this.width = width;
		this.height = height;
	}

	@Override
	public String toString() {
		return "Resize [width=" + width + ", height=" + height + ", keepRatio=" + keepRatio + ", size=" + size + "]";
	}

}
