package me.qyh.blog.template;

public final class PreviewTemplate implements Template {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final Template template;

	public Template getOriginalTemplate() {
		return template;
	}

	public PreviewTemplate(Template template) {
		super();
		this.template = template;
	}

	@Override
	public boolean isRoot() {
		return template.isRoot();
	}

	@Override
	public String getTemplate() {
		return template.getTemplate();
	}

	@Override
	public String getTemplateName() {
		return Template.TEMPLATE_PREVIEW_PREFIX + template.getTemplateName();
	}

	@Override
	public Template cloneTemplate() {
		return new PreviewTemplate(template);
	}

	@Override
	public boolean isCallable() {
		return template.isCallable();
	}

	@Override
	public boolean equalsTo(Template other) {
		return false;
	}
}