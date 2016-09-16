package me.qyh.blog.ui;

import javax.servlet.ServletContext;

import org.springframework.util.StringUtils;
import org.springframework.web.context.support.ServletContextResource;
import org.thymeleaf.exceptions.TemplateProcessingException;

import me.qyh.blog.message.Message;
import me.qyh.blog.ui.TplRender.TplRenderErrorDescriptionHandler;
import me.qyh.blog.ui.WidgetTagProcessor.WidgetTagParseException;
import me.qyh.blog.ui.page.Page;

public class DefaultTplRenderExceptionHandler implements TplRenderErrorDescriptionHandler {

	private static final String SPEL_EXPRESSION_ERROR_PREFIX = "Exception evaluating SpringEL expression:";
	/**
	 * @see ServletContextResource#getDescription()
	 */
	private static final String SERVLET_CONTEXT_RESOURCE_PREFIX = "ServletContext resource";
	private static final String STANDARD_EXPRESSION_ERROR_PREFIX = "Could not parse as expression:";

	@Override
	public TplRenderErrorDescription convert(Throwable directCause, ServletContext sc) {
		Throwable root = getEx(directCause);
		if (root instanceof WidgetTagParseException) {
			WidgetTagParseException ex = (WidgetTagParseException) root;
			return fromThrowable(ex.getOriginalThrowable(), sc, ex.getWidget());
		} else {
			return fromThrowable(directCause, sc, null);
		}
	}

	private TplRenderErrorDescription fromThrowable(Throwable throwable, ServletContext sc,
			String specificTemplateName) {
		TplRenderErrorDescription description = new TplRenderErrorDescription();
		TemplateProcessingException processEx = getProcessEx(throwable);
		// 如果不是TemplateProcessingException
		if (processEx == null) {
			// 查看是否是 StackOverflowError，也许不应该捕获这个error
			// 但这个操作很有可能是模板引用陷入死循环导致的
			if (throwable instanceof StackOverflowError) {
				description.setMessage(new Message("tplparse.overflow", "解析失败：可能引用存在死循环"));
			} else {
				description.setMessage(new Message("tplparse.fail", "解析失败"));
			}
		} else {
			// 如果行列号不存在，查找是否存在有行列号的异常
			if (processEx.getLine() == null) {
				TemplateProcessingException positionEx = getPositionEx(processEx);
				if (positionEx != null) {
					processEx = positionEx;
				}
			}
			// 如果还是不存在行列号
			if (processEx.getLine() == null) {
				description.setMessage(new Message("tplparse.fail", "解析失败"));
			} else {
				// 查找模板名
				String templateName = processEx.getTemplateName();
				processEx.setTemplateName(null);

				Page page = UIContext.get();

				description.setCol(processEx.getCol());
				description.setLine(processEx.getLine());
				description.setExpression(tryGetExpression(processEx.getMessage()));

				// 如果是页面模板
				if (!(page != null && page.getTemplateName().equals(templateName))) {
					// 模板不存在
					String findedTemplateName = templateName;

					if (findedTemplateName.startsWith(SERVLET_CONTEXT_RESOURCE_PREFIX)) {
						findedTemplateName = StringUtils.delete(templateName, SERVLET_CONTEXT_RESOURCE_PREFIX).trim();

						ServletContextResource scr = new ServletContextResource(sc, findedTemplateName);
						if (!scr.exists()) {
							description.setMessage(new Message("tplparse.tplNotExists",
									"模板" + findedTemplateName + "不存在", findedTemplateName));
						} else {
							description.addTemplateName(findedTemplateName);
						}
					} else {
						description.addTemplateName(findedTemplateName);
					}

				}
			}
		}
		if (specificTemplateName != null) {
			description.getTemplateNames().add(0, specificTemplateName);
		}
		return description;
	}

	private String tryGetExpression(String errorMsg) {
		if (errorMsg.startsWith(SPEL_EXPRESSION_ERROR_PREFIX)) {
			String expression = StringUtils.delete(errorMsg, SPEL_EXPRESSION_ERROR_PREFIX).trim();
			return expression.substring(1, expression.length() - 1);
		}
		if (errorMsg.startsWith(STANDARD_EXPRESSION_ERROR_PREFIX)) {
			String expression = StringUtils.delete(errorMsg, STANDARD_EXPRESSION_ERROR_PREFIX).trim();
			return expression.substring(1, expression.length() - 1);
		}
		return null;
	}

	private TemplateProcessingException getProcessEx(Throwable e) {
		if (e == null) {
			return null;
		}
		if (e instanceof TemplateProcessingException) {
			return (TemplateProcessingException) e;
		}
		return getProcessEx(e.getCause());
	}

	private TemplateProcessingException getPositionEx(Throwable e) {
		Throwable cause = e.getCause();
		if (cause == null) {
			return null;
		} else {
			if (cause instanceof TemplateProcessingException) {
				TemplateProcessingException tpe = (TemplateProcessingException) cause;
				if (tpe.getLine() != null) {
					return tpe;
				}
			}
			return getPositionEx(cause);
		}
	}

	private Throwable getEx(Throwable e) {
		Throwable cause = e.getCause();
		if (cause == null) {
			return e;
		} else {
			return getEx(cause);
		}
	}
}