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
package me.qyh.blog.util;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

@UIUtils
public final class Nums {
	public static int randomInt(int min, int max) {
		return ThreadLocalRandom.current().nextInt(min, max + 1);
	}

	public static int max(int... nums) {
		return IntStream.of(nums).max().orElse(0);
	}

	public static int min(int... nums) {
		return IntStream.of(nums).min().orElse(0);
	}

	public static int max(List<Integer> numList) {
		return numList.stream().mapToInt(Integer::intValue).max().orElse(0);
	}

	public static int min(List<Integer> numList) {
		return numList.stream().mapToInt(Integer::intValue).min().orElse(0);
	}

	private Nums() {

	}
}
