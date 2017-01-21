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

/**
 * 校验辅助
 * 
 * @author Administrator
 *
 */
@UIUtils
public final class Validators {

	/**
	 * private
	 */
	private Validators() {
		super();
	}

	/**
	 * 判断字符串是否为null或空
	 * 
	 * @param str
	 *            字符串
	 * @param trim
	 *            是否trim
	 * @return 如果为null|空 返回true,否则返回false
	 */
	public static boolean isEmptyOrNull(String str, boolean trim) {
		if (str == null) {
			return true;
		}
		return trim ? str.trim().isEmpty() : str.isEmpty();
	}

	/**
	 * 判断两个对象是否是相同的类型，如果两个对象任意一个为null，那么返回false
	 * 
	 * @param a
	 *            对象a
	 * @param b
	 *            对象b
	 * @return
	 */
	public static boolean baseEquals(Object a, Object b) {
		if (a == null) {
			return false;
		}
		if (b == null) {
			return false;
		}
		if (a == b) {
			return true;
		}
		if (a.getClass() != b.getClass()) {
			return false;
		}
		return true;
	}
}
