package me.qyh.blog.ui;

import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import org.thymeleaf.context.IEngineContext;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.engine.TemplateModel;
import org.thymeleaf.model.IAttribute;
import org.thymeleaf.model.IModel;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.processor.element.AbstractElementTagProcessor;
import org.thymeleaf.processor.element.IElementTagStructureHandler;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.util.FastStringWriter;

import me.qyh.blog.ui.page.Page;
import me.qyh.blog.ui.widget.WidgetTpl;

/**
 * {@link http://www.thymeleaf.org/doc/tutorials/3.0/extendingthymeleaf.html#creating-our-own-dialect}
 * 
 * @author mhlx
 *
 */
public class WidgetTagProcessor extends AbstractElementTagProcessor {

	private static final String TAG_NAME = "widget";
	private static final String ATTRIBUTES = "attributes";
	private static final int PRECEDENCE = 1000;
	private static final String NAME = "name";

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
		Page page = UIContext.get();
		if (page != null) {
			IAttribute nameAtt = tag.getAttribute(NAME);
			if (nameAtt != null) {
				String name = nameAtt.getValue();
				WidgetTpl tpl = page.getWidgetTpl(name);
				if (tpl != null) {
					Attributes attributes = handleAttributes(tag);
					if (attributes == null) {
						attributes = new Attributes();
					}
					IEngineContext _context = (IEngineContext) context;
					_context.setVariable(ATTRIBUTES, attributes);
					try {
						IModel model = context.getModelFactory().parse(context.getTemplateData(), tpl.getTpl());
						Writer writer = new FastStringWriter(200);
						context.getConfiguration().getTemplateManager().process((TemplateModel) model, _context,
								writer);
						structureHandler.replaceWith(writer.toString(), false);
					} catch (Throwable e) {
						throw new WidgetTagParseException(e, name);
					} finally {
						_context.removeVariable(ATTRIBUTES);
					}
					return;
				}
			}
		}
		structureHandler.removeElement();
	}

	public static final class WidgetTagParseException extends RuntimeException {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private Throwable originalThrowable;
		private String widget;

		public WidgetTagParseException(Throwable originalThrowable, String widget) {
			this.originalThrowable = originalThrowable;
			this.widget = widget;
		}

		public Throwable getOriginalThrowable() {
			return originalThrowable;
		}

		public String getWidget() {
			return widget;
		}

	}

	protected Attributes handleAttributes(IProcessableElementTag tag) {
		Attributes attributes = new Attributes();
		IAttribute[] attributArray = tag.getAllAttributes();
		for (IAttribute attribute : attributArray) {
			String v = attribute.getValue();
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
