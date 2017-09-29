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
package me.qyh.blog.comment.vo;

import java.util.HashMap;
import java.util.Map;

public class CommentStatistics {

	private Map<String, Integer> stMap = new HashMap<>();

	public Map<String, Integer> getStMap() {
		return stMap;
	}

	public void setStMap(Map<String, Integer> stMap) {
		this.stMap = stMap;
	}
	/**
	 * 适配以前
	 * @return
	 */
	public int getTotalArticleComments() {
		return getComments("article");
	}
	/**
	 * 适配以前
	 * @return
	 */
	public int getTotalPageComments() {
		return getComments("userpage");
	}

	public int getComments(String key) {
		Integer count = stMap.get(key);
		return count == null ? 0 : count;
	}

}
