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
package me.qyh.blog.ui;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import me.qyh.blog.config.UrlHelper;
import me.qyh.blog.message.Messages;
import me.qyh.blog.security.UserContext;
import me.qyh.blog.web.interceptor.SpaceContext;

public class UIExposeHelper {

	@Autowired
	private UrlHelper urlHelper;
	@Autowired
	private Messages messages;

	private final _Collections collections = new _Collections();
	private final _Nums nums = new _Nums();

	public Map<String, Object> getHelpers(HttpServletRequest request) {
		Map<String, Object> helpers = Maps.newHashMap();
		helpers.put("urls", urlHelper.getUrls(request));
		helpers.put("user", UserContext.get());
		helpers.put("messages", messages);
		helpers.put("space", SpaceContext.get());

		helpers.put("collections", collections);
		helpers.put("nums", nums);
		return helpers;
	}

	public final class _Nums {
		public int randomInt(int min, int max) {
			return ThreadLocalRandom.current().nextInt(min, max + 1);
		}

		public int max(int... nums) {
			return NumberUtils.max(nums);
		}

		public int min(int... nums) {
			return NumberUtils.min(nums);
		}

		public int max(List<Integer> numList) {
			int[] nums = new int[numList.size()];
			for (int i = 0; i < numList.size(); i++) {
				nums[i] = numList.get(i);
			}
			return NumberUtils.max(nums);
		}

		public int min(List<Integer> numList) {
			int[] nums = new int[numList.size()];
			for (int i = 0; i < numList.size(); i++) {
				nums[i] = numList.get(i);
			}
			return NumberUtils.min(nums);
		}

		private _Nums() {

		}
	}

	public final class _Collections {

		/**
		 * 打乱一个list
		 * 
		 * @param list
		 * @param clone
		 *            如果为true，返回ArrayList(oldList);
		 * @return
		 */
		public List<?> shuffle(List<?> list, boolean clone) {
			List<?> toShuffle = clone ? Lists.newArrayList(list) : list;
			Collections.shuffle(toShuffle, new Random(System.nanoTime()));
			return toShuffle;
		}

		private _Collections() {

		}

	}
}
