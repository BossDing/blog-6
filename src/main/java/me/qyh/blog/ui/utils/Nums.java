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
