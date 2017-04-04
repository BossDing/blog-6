/*
 * Copyright 2016 qyh.me
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package me.qyh.blog.core.thymeleaf;

import java.util.List;
import java.util.Optional;

import org.springframework.util.StringUtils;
import org.springframework.web.context.support.ServletContextResource;
import org.thymeleaf.exceptions.TemplateProcessingException;

import me.qyh.blog.core.exception.RuntimeLogicException;
import me.qyh.blog.core.exception.SystemException;
import me.qyh.blog.core.lock.LockException;
import me.qyh.blog.core.security.AuthencationException;
import me.qyh.blog.core.thymeleaf.TplRenderErrorDescription.TemplateErrorInfo;
import me.qyh.blog.core.thymeleaf.dialect.RedirectException;
import me.qyh.blog.util.ExceptionUtils;
import me.qyh.blog.util.Validators;

public final class TemplateExceptionUtils {

	private static final String SPEL_EXPRESSION_ERROR_PREFIX = "Exception evaluating SpringEL expression:";
	/**
	 * @see ServletContextResource#getDescription()
	 */
	private static final String STANDARD_EXPRESSION_ERROR_PREFIX = "Could not parse as expression:";

	private static final String SERVLET_RESOURCE_PREFIX = "ServletContext resource ";

	public static Exception convert(String templateName, Throwable e) {
		if (e instanceof TemplateProcessingException) {
			Optional<Throwable> finded = ExceptionUtils.getFromChain(e, RuntimeLogicException.class,
					LockException.class, AuthencationException.class, RedirectException.class);
			if (finded.isPresent()) {
				return (Exception) finded.get();
			}
			return new TplRenderException(templateName, fromException((TemplateProcessingException) e, templateName),
					e);
		}
		if (e instanceof UIStackoverflowError) {
			UIStackoverflowError error = (UIStackoverflowError) e;
			return new TplRenderException(templateName, fromError(error), e);
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
		List<Throwable> ths = ExceptionUtils.getThrowableList(e);
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
		}
		return name;
	}
}
