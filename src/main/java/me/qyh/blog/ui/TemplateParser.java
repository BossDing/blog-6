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

import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Tag;
import org.jsoup.select.Elements;
import org.springframework.util.CollectionUtils;

import me.qyh.blog.exception.LogicException;

/**
 * 
 * @see DataTagProcessor
 * @see FragmentTagProcessor
 * @author Administrator
 *
 */
public final class TemplateParser {

	private static final String DATA_TAG = "data";
	private static final String FRAGEMENT = "fragment";
	private static final String NAME_ATTR = "name";

	/**
	 * 如果data标签拥有该属性，则在页面加载时候获取，依赖该属性的fragment必须放在该属性标签之后！！！
	 */
	private static final String DATA_DYNAMIC_ATT = "dynamic";

	public static ParseResult parse(String tpl) throws LogicException {
		ParseResult result = new ParseResult();
		Document doc = Jsoup.parse(tpl);
		clean(doc);
		Elements dataEles = doc.getElementsByTag(DATA_TAG);
		for (Element dataEle : dataEles) {
			String name = dataEle.attr(NAME_ATTR);
			String dynamicAtt = dataEle.attr(DATA_DYNAMIC_ATT);
			boolean dynamic = Boolean.parseBoolean(dynamicAtt);
			// 不解析动态的，利用DataTagProcessor解析
			if (dynamic) {
				continue;
			}
			DataTag tag = new DataTag(name);
			Attributes attributes = dataEle.attributes();
			if (attributes != null) {
				for (Attribute attribute : attributes) {
					tag.put(attribute.getKey(), attribute.getValue());
				}
			}
			result.addDataTag(tag);
		}
		Elements fragmentEles = doc.getElementsByTag(FRAGEMENT);
		for (Element fragmentEle : fragmentEles) {
			String name = fragmentEle.attr(NAME_ATTR);
			result.addFragment(name);
		}
		return result;
	}

	public static String buildFragmentTag(String name, Map<String, String> atts) {
		Tag tag = Tag.valueOf(FRAGEMENT);
		Attributes attributes = new Attributes();
		if (!CollectionUtils.isEmpty(atts)) {
			for (Map.Entry<String, String> it : atts.entrySet()) {
				attributes.put(it.getKey(), it.getValue());
			}
		}
		attributes.put(NAME_ATTR, name);
		Element ele = new Element(tag, "", attributes);
		return ele.toString();
	}

	private static void clean(Document doc) {
		// 删除包含挂件自标签的挂件标签
		doc.select("data:has(data)").remove();
		doc.select("fragment:has(fragment)").remove();
		// 删除没有包含name属性的标签
		doc.select("data:not([name])").remove();
		doc.select("fragment:not([name])").remove();
		// 删除属性为空的标签
		doc.select("data[name~=^$]").remove();
		doc.select("fragment[name~=^$]").remove();
	}
}
