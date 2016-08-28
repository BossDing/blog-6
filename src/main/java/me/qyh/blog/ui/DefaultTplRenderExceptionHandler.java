package me.qyh.blog.ui;

import java.io.FileNotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.thymeleaf.exceptions.TemplateProcessingException;

import me.qyh.blog.message.Message;
import me.qyh.blog.ui.TplRender.TplRenderExceptionHandler;

public class DefaultTplRenderExceptionHandler implements TplRenderExceptionHandler {

	private static final String SPEL_EXPRESSION_ERROR_PREFIX = "Exception evaluating SpringEL expression:";
	private static final String SERVLET_CONTEXT_RESOURCE_PREFIX = "ServletContext resource";
	private static final String STANDARD_EXPRESSION_ERROR_PREFIX = "Could not parse as expression:";

	private static final Logger logger = LoggerFactory.getLogger(DefaultTplRenderExceptionHandler.class);

	@Override
	public TplRenderException convertThrowable(Throwable directCause) {
		// 从错误中获取TemplateProcessingException(带有行列号)
		TemplateProcessingException tpe = getProcessEx(directCause);
		TplRenderErrorDescription description = new TplRenderErrorDescription();
		if (tpe != null) {
			if (tpe.getLine() == null) {
				TemplateProcessingException positionEx = getPositionEx(tpe);
				if (positionEx != null) {
					tpe = positionEx;
				}
			}
			if (tpe.getLine() == null) {
				description.setMessage(new Message("tplparse.fail", "解析失败"));
			} else {
				description.setCol(tpe.getCol());
				description.setLine(tpe.getLine());
				String templateName = tpe.getTemplateName();
				tpe.setTemplateName(null);
				description.setExpression(tryGetExpression(tpe.getMessage()));
				Template template = findTemplate(templateName);
				if (template != null) {
					// 这里需要重新获取用户自定义的模板内容，因为渲染之后和用户当前输入的内容行列是不一样的
					description.setTemplate(template);
				} else {
					Throwable root = getEx(tpe);
					if (root instanceof FileNotFoundException) {
						String msg = root.getMessage();
						// 模板不存在
						if (msg.contains(templateName)) {
							Template tpl = UIContext.get();
							if (tpl != null) {
								description.setTemplate(tpl);
							}
						}
					}
					if (description.getTemplate() == null) {
						// 调用了系统模板，无需获取具体内容
						description.setTemplateName(getTemplateName(templateName));
					}
				}
			}
		} else {
			// 如果不是TemplateProcessingException
			// 查看是否是 StackOverflowError，也许不应该捕获这个error 但这个操作很有可能是模板引用陷入死循环导致的
			if (directCause instanceof StackOverflowError) {
				description.setMessage(new Message("tplparse.overflow", "解析失败：可能引用存在死循环"));
			} else {
				// 如果不是上述异常，可能模板渲染出现异常
				logger.error(directCause.getMessage(), directCause);
				// Throwable root = getEx(directCause);
				description.setMessage(new Message("tplparse.fail", "解析失败"));
			}
		}
		return new TplRenderException(description);
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

	private String getTemplateName(String templateName) {
		if (templateName.startsWith(SERVLET_CONTEXT_RESOURCE_PREFIX)) {
			String tn = StringUtils.delete(templateName, SERVLET_CONTEXT_RESOURCE_PREFIX).trim();
			return tn.substring(1, tn.length() - 1);
		}
		return templateName;
	}

	private Template findTemplate(String templateName) {
		Template template = UIContext.get();
		if (template != null) {
			Template find = template.find(templateName);
			if (find != null) {
				return find;
			}
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