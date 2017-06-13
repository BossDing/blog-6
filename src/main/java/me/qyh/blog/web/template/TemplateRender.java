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
package me.qyh.blog.web.template;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

import me.qyh.blog.core.exception.RuntimeLogicException;
import me.qyh.blog.core.exception.SystemException;
import me.qyh.blog.core.lock.LockException;
import me.qyh.blog.core.security.AuthencationException;
import me.qyh.blog.util.ExceptionUtils;

/**
 * 用来将模板解析成字符串
 * 
 * @author Administrator
 *
 */
@Component
public final class TemplateRender {

	@Autowired
	private TemplateExposeHelper uiExposeHelper;
	@Autowired
	private PlatformTransactionManager transactionManager;
	@Autowired
	private TemplateRenderExecutor templateRenderer;
	@Autowired
	private TemplateExceptionTranslater templateExceptionTranslater;

	public String render(String templateName, Map<String, Object> model, HttpServletRequest request,
			HttpServletResponse response, ParseConfig config) throws TemplateRenderException {
		try {
			return doRender(templateName, model == null ? new HashMap<>() : model, request, response, config);
		} catch (TemplateRenderException | RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new SystemException(e.getMessage(), e);
		}
	}

	public String doRender(String templateName, Map<String, Object> model, HttpServletRequest request,
			HttpServletResponse response, ParseConfig config) throws Exception {
		uiExposeHelper.addVariables(request);
		ParseContext.setConfig(config);
		try {
			return doRender(templateName, model, request, response);
		} catch (Throwable e) {
			markRollBack();

			// 从异常栈中寻找 逻辑异常
			Optional<Throwable> finded = ExceptionUtils.getFromChain(e, RuntimeLogicException.class,
					LockException.class, AuthencationException.class, RedirectException.class);
			if (finded.isPresent()) {
				throw (Exception) finded.get();
			}

			// 如果没有逻辑异常，转化模板异常
			TemplateRenderException templateRenderException = templateExceptionTranslater.translate(templateName, e);
			if (templateRenderException != null) {
				throw templateRenderException;
			}

			throw new SystemException(e.getMessage(), e);

		} finally {
			commit();
			ParseContext.remove();
		}
	}

	private void markRollBack() {
		TransactionStatus status = ParseContext.getTransactionStatus();
		if (status != null) {
			status.setRollbackOnly();
		}
	}

	private void commit() {
		TransactionStatus status = ParseContext.getTransactionStatus();
		if (status != null) {
			transactionManager.commit(status);
		}
	}

	private String doRender(String viewTemplateName, final Map<String, Object> model, final HttpServletRequest request,
			final HttpServletResponse response) throws Exception {
		return templateRenderer.execute(viewTemplateName, model, request, new ReadOnlyResponse(response));
	}

}
