package me.qyh.blog.ui;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import me.qyh.blog.exception.LogicException;
import me.qyh.blog.ui.data.DataBind;
import me.qyh.blog.ui.fragement.Fragement;

/**
 * 
 * @author Administrator
 *
 */
public final class TemplateParser {

	private static final String DATA_TAG = "data";
	private static final String FRAGEMENT = "fragement";
	private static final String NAME_ATTR = "name";

	public interface DataQuery {
		/**
		 * 根据用户的widget标签查询对应的widgetTpl
		 * 
		 * @param dataTag
		 *            widget标签，不会为null
		 * @return
		 * @throws LogicException
		 * @throws MissParamException
		 */
		DataBind<?> query(DataTag dataTag) throws LogicException;
	}

	public interface FragementQuery {

		Fragement query(String name);
	}

	public DataTag parse(String dataTagStr) {
		Element body = Jsoup.parseBodyFragment(dataTagStr).body();
		Elements eles = body.children();
		if (eles.size() == 1) {
			Element ele = eles.get(0);
			if (DATA_TAG.equals(ele.tagName())) {
				String name = ele.attr(NAME_ATTR);
				if (name != null) {
					DataTag dataTag = new DataTag(name);
					Attributes attributes = ele.attributes();
					if (attributes != null) {
						for (Attribute attribute : attributes) {
							dataTag.put(attribute.getKey(), attribute.getValue());
						}
					}
					return dataTag;
				}
			}
		}
		return null;
	}

	public ParseResult parse(String tpl, DataQuery dquery, FragementQuery fquery) throws LogicException {
		ParseResult result = new ParseResult();
		Document doc = Jsoup.parse(tpl);
		clean(doc);
		Elements dataEles = doc.getElementsByTag(DATA_TAG);
		Map<DataTag, DataBind<?>> cache = new LinkedHashMap<DataTag, DataBind<?>>();
		for (Element dataEle : dataEles) {
			String name = dataEle.attr(NAME_ATTR);
			DataTag tag = new DataTag(name);
			Attributes attributes = dataEle.attributes();
			if (attributes != null) {
				for (Attribute attribute : attributes) {
					tag.put(attribute.getKey(), attribute.getValue());
				}
			}
			DataBind<?> bind = !cache.containsKey(tag) ? dquery.query(tag) : cache.get(tag);
			if (bind != null) {
				cache.put(tag, bind);
				dataEle.removeAttr(name);
			} else {
				result.addUnkownData(tag);
				removeElement(dataEle);
			}
		}
		Elements fragementEles = doc.getElementsByTag(FRAGEMENT);
		for (Element fragementEle : fragementEles) {
			String name = fragementEle.attr(NAME_ATTR);
			Fragement fragement = fquery.query(name);
			if (fragement == null) {
				result.addUnkownFragement(name);
				removeElement(fragementEle);
			} else {
				result.putFragement(name, fragement);
			}
		}
		result.setBinds(new ArrayList<>(cache.values()));
		return result;
	}

	private void clean(Document doc) {
		// 删除包含挂件自标签的挂件标签
		doc.select("data:has(data)").remove();
		doc.select("fragement:has(fragement)").remove();
		// 删除没有包含name属性的标签
		doc.select("data:not([name])").remove();
		doc.select("fragement:not([name])").remove();
		// 删除属性为空的标签
		doc.select("data[name~=^$]").remove();
		doc.select("fragement[name~=^$]").remove();
	}

	private void removeElement(Element e) {
		try {
			e.remove();
		} catch (Exception ex) {

		}
	}

	public static void main(String[] args) {
		new TemplateParser().parse("<div></div>");
	}
}
