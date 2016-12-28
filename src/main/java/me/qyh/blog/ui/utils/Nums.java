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
import java.util.concurrent.ThreadLocalRandom;

import org.apache.commons.lang3.math.NumberUtils;

public final class Nums {
	public static int randomInt(int min, int max) {
		return ThreadLocalRandom.current().nextInt(min, max + 1);
	}

	public static int max(int... nums) {
		return NumberUtils.max(nums);
	}

	public static int min(int... nums) {
		return NumberUtils.min(nums);
	}

	public static int max(List<Integer> numList) {
		int[] nums = new int[numList.size()];
		for (int i = 0; i < numList.size(); i++) {
			nums[i] = numList.get(i);
		}
		return NumberUtils.max(nums);
	}

	public static int min(List<Integer> numList) {
		int[] nums = new int[numList.size()];
		for (int i = 0; i < numList.size(); i++) {
			nums[i] = numList.get(i);
		}
		return NumberUtils.min(nums);
	}

	private Nums() {

	}
}
