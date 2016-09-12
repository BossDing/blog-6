package me.qyh.blog.ui;

import java.util.HashMap;
import java.util.Map;

import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.model.IAttribute;
import org.thymeleaf.model.IModel;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.processor.element.AbstractElementTagProcessor;
import org.thymeleaf.processor.element.IElementTagStructureHandler;
import org.thymeleaf.templatemode.TemplateMode;

/**
 * {@link http://www.thymeleaf.org/doc/tutorials/3.0/extendingthymeleaf.html#creating-our-own-dialect}
 * 
 * @author mhlx
 *
 */
public class WidgetTagProcessor extends AbstractElementTagProcessor {

	private static final String TAG_NAME = "widget";
	public static final String TEMPLATE_NAME = "templatename";
	private static final String ATTRIBUTES = "attributes";
	private static final int PRECEDENCE = 1000;

	public WidgetTagProcessor(String dialectPrefix) {
		super(TemplateMode.HTML, // This processor will apply only to HTML mode
				dialectPrefix, // Prefix to be applied to name for matching
				TAG_NAME, // Tag name: match specifically this tag
				false, // Apply dialect prefix to tag name
				null, // No attribute name: will match by tag name
				false, // No prefix to be applied to attribute name
				PRECEDENCE); // Precedence (inside dialect's own precedence)
	}

	@Override
	protected final void doProcess(ITemplateContext context, IProcessableElementTag tag,
			IElementTagStructureHandler structureHandler) {
		Template tpl = UIContext.get();
		if (tpl != null) {
			String templateName = tag.getAttributeValue(TEMPLATE_NAME);
			Template finded = tpl.find(templateName);
			if (finded != null) {
				Attributes attributes = handleAttributes(tag);
				if (attributes == null) {
					attributes = new Attributes();
				}
				structureHandler.setLocalVariable(ATTRIBUTES, attributes);
				IModel model = context.getModelFactory().parse(context.getTemplateData(), finded.getTpl());
				structureHandler.replaceWith(model, true);
			}
		}
	}

	protected Attributes handleAttributes(IProcessableElementTag tag) {
		Attributes attributes = new Attributes();
		IAttribute[] attributArray = tag.getAllAttributes();
		for (IAttribute attribute : attributArray) {
			String v = attribute.getAttributeCompleteName();
			if (v != null) {
				attributes.put(attribute.getAttributeCompleteName(), v);
			}
		}
		return attributes;
	}

	protected class Attributes {
		private Map<String, String> attributes = new HashMap<>();

		public String get(String key) {
			return attributes.get(key);
		}

		public double getDouble(String key, double _default) {
			String v = get(key);
			if (v != null) {
				return Double.parseDouble(v);
			}
			return _default;
		}

		public long getLong(String key, long _default) {
			String v = get(key);
			if (v != null) {
				return Long.parseLong(v);
			}
			return _default;
		}

		public int getInt(String key, int _default) {
			String v = get(key);
			if (v != null) {
				return Integer.parseInt(v);
			}
			return _default;
		}

		public boolean getBoolean(String key, boolean _default) {
			String v = get(key);
			if (v != null) {
				return Boolean.parseBoolean(v);
			}
			return _default;
		}

		public Attributes() {
		}

		public void put(String key, String v) {
			attributes.put(key, v);
		}
	}

}
