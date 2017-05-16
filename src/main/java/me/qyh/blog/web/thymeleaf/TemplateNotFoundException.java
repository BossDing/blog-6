package me.qyh.blog.web.thymeleaf;

/**
 * 没有发现模板异常
 * <p>
 * <b>逻辑异常，无需日志记录</b>
 * </p>
 * 
 * @author mhlx
 *
 */
public final class TemplateNotFoundException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private final String templateName;

	public TemplateNotFoundException(String templateName) {
		super();
		this.templateName = templateName;
	}

	public String getTemplateName() {
		return templateName;
	}

	@Override
	public synchronized Throwable fillInStackTrace() {
		return this;
	}

}
