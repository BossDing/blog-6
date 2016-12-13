package me.qyh.blog.ui.utils;

import java.util.List;
import java.util.Random;

import com.google.common.collect.Lists;

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