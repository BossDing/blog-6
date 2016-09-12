package me.qyh.blog.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import me.qyh.blog.exception.LogicException;
import me.qyh.blog.ui.widget.WidgetTag;
import me.qyh.blog.ui.widget.WidgetTpl;

/**
 * 将widget标签转为widget数据
 * 
 * @author Administrator
 *
 */
public class DefaultTemplateParser implements TemplateParser {

	private static final Logger logger = LoggerFactory.getLogger(DefaultTemplateParser.class);

	@Override
	public ParseResult parse(String tpl, WidgetQuery query) throws LogicException {
		Document doc = Jsoup.parse(tpl);
		clean(doc);
		Elements eles = doc.getElementsByTag("widget");
		Map<String, WidgetTpl> widgets = new HashMap<String, WidgetTpl>();
		Set<String> unknowWidgets = new HashSet<>();
		if (!eles.isEmpty()) {
			for (Element ele : eles) {
				String name = ele.attr("name");
				WidgetTpl widget = !widgets.containsKey(name) ? query.query(new WidgetTag(name)) : widgets.get(name);
				if (widget != null) {
					widgets.put(name, widget);
					ele.attr(WidgetTagProcessor.TEMPLATE_NAME, widget.getTemplateName());
				} else {
					// 挂件不存在
					unknowWidgets.add(name);
					Elements noWidgets = ele.select("nowidget");
					if (!noWidgets.isEmpty()) {
						ele.after(noWidgets.get(0).html());
					}
					removeElement(ele);
				}
			}
		}
		String parsed = doc.html();
		logger.debug("模板解析完毕，解析之后的模板为:" + parsed);
		return new ParseResult(parsed, new ArrayList<WidgetTpl>(widgets.values()), unknowWidgets);
	}

	protected void clean(Document doc) {
		// 删除包含挂件自标签的挂件标签
		doc.select("widget:has(widget)").remove();
		doc.select("nowidget:has(nowidget)").remove();
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

}
