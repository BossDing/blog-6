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
package me.qyh.blog.ui.utils;

import java.util.List;
import java.util.Random;

import com.google.common.collect.Lists;

@UIUtils
public final class Collections {

	private Collections() {
		super();
	}

	/**
	 * 打乱一个list
	 * 
	 * @param list
	 * @param clone
	 *            如果为true，返回ArrayList(oldList);
	 * @return
	 */
	public static List<?> shuffle(List<?> list, boolean clone) {
		List<?> toShuffle = clone ? Lists.newArrayList(list) : list;
		java.util.Collections.shuffle(toShuffle, new Random(System.nanoTime()));
		return toShuffle;
	}

}