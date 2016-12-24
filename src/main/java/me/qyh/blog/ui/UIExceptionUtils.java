package me.qyh.blog.ui;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.context.support.ServletContextResource;
import org.thymeleaf.exceptions.TemplateProcessingException;

import me.qyh.blog.exception.RuntimeLogicException;
import me.qyh.blog.exception.SystemException;
import me.qyh.blog.lock.LockException;
import me.qyh.blog.security.AuthencationException;
import me.qyh.blog.ui.TplRenderErrorDescription.TemplateErrorInfo;
import me.qyh.blog.util.Validators;

public final class UIExceptionUtils {

	private static final String SPEL_EXPRESSION_ERROR_PREFIX = "Exception evaluating SpringEL expression:";
	/**
	 * @see ServletContextResource#getDescription()
	 */
	private static final String STANDARD_EXPRESSION_ERROR_PREFIX = "Could not parse as expression:";

	private static final String SERVLET_RESOURCE_PREFIX = "ServletContext resource ";

	public static Exception convert(String templateName, Throwable e) {
		if (e instanceof TemplateProcessingException) {
			Throwable cause = e.getCause();
			if (cause instanceof RuntimeLogicException) {
				return ((RuntimeLogicException) cause);
			}
			if (cause instanceof LockException) {
				return (LockException) cause;
			}
			if (cause instanceof AuthencationException) {
				return (AuthencationException) cause;
			}
			return new TplRenderException(fromException((TemplateProcessingException) e, templateName), e);
		}
		if (e instanceof UIStackoverflowError) {
			UIStackoverflowError error = (UIStackoverflowError) e;
			return new TplRenderException(fromError(error), e);
		}
		return new SystemException(e.getMessage(), e);
	}

	private static TplRenderErrorDescription fromError(UIStackoverflowError e) {
		TplRenderErrorDescription description = new TplRenderErrorDescription();
		description.addTemplateErrorInfos(
				new TemplateErrorInfo(parseTemplateName(e.getTemplateName()), e.getLine(), e.getCol()));
		return description;
	}

	private static TplRenderErrorDescription fromException(TemplateProcessingException e, String templateName) {
		TplRenderErrorDescription description = new TplRenderErrorDescription();
		Throwable[] ths = ExceptionUtils.getThrowables(e);
		TemplateProcessingException last = null;
		for (Throwable th : ths) {
			if (TemplateProcessingException.class.isAssignableFrom(th.getClass())) {
				TemplateProcessingException templateProcessingException = (TemplateProcessingException) th;
				String templateName2 = templateProcessingException.getTemplateName();
				if (!Validators.isEmptyOrNull(templateName2, true)) {
					templateName2 = parseTemplateName(templateName2);
					description.addTemplateErrorInfos(new TemplateErrorInfo(templateName2,
							templateProcessingException.getLine(), templateProcessingException.getCol()));
					last = templateProcessingException;
				}
			}
		}
		if (last != null) {
			last.setTemplateName(null);
			description.setExpression(tryGetExpression(last.getMessage()));
		}
		return description;
	}

	private static String tryGetExpression(String errorMsg) {
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

	private static String parseTemplateName(String name) {
		if (!Validators.isEmptyOrNull(name, true)) {
			if (name.startsWith(SERVLET_RESOURCE_PREFIX)) {
				int first = name.indexOf('[');
				int last = name.lastIndexOf(']');
				if (first != -1 && last != -1) {
					name = name.substring(first + 1, last);
				}
			}
			if (TemplateUtils.isFragmentTemplate(name)) {
				name = "Fragment:" + TemplateUtils.getFragmentName(name);
			}
		}
		return name;
	}
}
