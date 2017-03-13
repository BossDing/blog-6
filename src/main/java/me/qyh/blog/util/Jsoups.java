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
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

@UIUtils
public final class Jsoups {

	private Jsoups() {
		super();
	}

	/**
	 * 将html文本解析成document
	 * 
	 * @param html
	 * @return
	 */
	public static Document parse(String html) {
		return Jsoup.parse(html);
	}

	/**
	 * 获取html中的图片地址
	 * <p>
	 * &lt;img src="src"/&gt;
	 * </p>
	 * 
	 * @param html
	 * @return
	 */
	public static List<String> getImgs(String html) {
		return getAttrs(getTags(html, "img"), "src");
	}

	/**
	 * 获取html中的超链接地址
	 * <p>
	 * &lt;a href="href"&gt;LINK&lt;/a&gt;
	 * </p>
	 * 
	 * @param html
	 * @return
	 */
	public static List<String> getLinks(String html) {
		return getAttrs(getTags(html, "a"), "href");
	}

	/**
	 * 获取html中的某个标签元素
	 * 
	 * @param html
	 *            html文本
	 * @param tag
	 *            标签名
	 * @return
	 */
	public static Elements getTags(String html, String tag) {
		return parse(html).getElementsByTag(tag);
	}

	/**
	 * 获取元素集合中的某属性
	 * 
	 * @param eles
	 *            元素集合
	 * @param attr
	 *            属性名
	 * @return
	 */
	public static List<String> getAttrs(Elements eles, String attr) {
		return eles.stream().map(ele -> ele.attr(attr)).collect(Collectors.toList());
	}

}
