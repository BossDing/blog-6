package me.qyh.blog.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import me.qyh.blog.exception.LogicException;
import me.qyh.blog.ui.widget.WidgetTpl;

/**
 * 将widget标签转为widget数据
 * 
 * @author Administrator
 *
 */
public class DefaultTemplateParser implements TemplateParser {

	@Override
	public ParseResult parse(String tpl, WidgetQuery query) throws LogicException {
		Document doc = Jsoup.parse(tpl);
		clean(doc);
		Elements eles = doc.getElementsByTag("widget");
		Map<WidgetTag, WidgetTpl> widgets = new HashMap<WidgetTag, WidgetTpl>();
		Set<String> unknowWidgets = new HashSet<>();
		if (!eles.isEmpty()) {
			int order = 0;
			for (Element ele : eles) {
				String name = ele.attr("name");
				WidgetTag tag = new WidgetTag(name);
				Attributes attributes = ele.attributes();
				if (attributes != null) {
					for (Attribute attribute : attributes) {
						tag.put(attribute.getKey(), attribute.getValue());
					}
				}
				WidgetTpl widget = !widgets.containsKey(tag) ? query.query(tag) : widgets.get(tag);
				if (widget != null) {
					int eleOrder = getOrder(ele);
					widget.setOrder(eleOrder == -1 ? order : eleOrder);
					widgets.put(tag, widget);
					ele.removeAttr(name);
				} else {
					// 挂件不存在
					unknowWidgets.add(name);
					removeElement(ele);
				}
			}
		}
		return new ParseResult(new ArrayList<WidgetTpl>(widgets.values()), unknowWidgets);
	}

	protected void clean(Document doc) {
		// 删除包含挂件自标签的挂件标签
		doc.select("widget:has(widget)").remove();
		// 删除没有包含name属性的widget标签
		doc.select("widget:not([name])").remove();
		// 删除属性为空的标签
		doc.select("widget[name~=^$]").remove();
	}

	private void removeElement(Element e) {
		try {
			e.remove();
		} catch (Exception ex) {

		}
	}

	private int getOrder(Element e) {
		String order = e.attr("order");
		if (order != null) {
			try {
				return Integer.parseInt(order);
			} catch (Exception ex) {
			}
		}
		return -1;
	}

}
