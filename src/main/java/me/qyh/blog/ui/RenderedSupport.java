package me.qyh.blog.ui;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.servlet.View;
import org.thymeleaf.spring4.view.ThymeleafViewResolver;
import org.thymeleaf.util.FastStringWriter;

import me.qyh.blog.ui.dialect.TransactionContext;

public class RenderedSupport {

	@Autowired
	protected ThymeleafViewResolver thymeleafViewResolver;
	@Autowired
	private UIExposeHelper uiExposeHelper;
	@Autowired
	private PlatformTransactionManager transactionManager;

	private static final Logger logger = LoggerFactory.getLogger(RenderedSupport.class);

	protected String render(String templateName, Map<String, Object> model, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		View view = thymeleafViewResolver.resolveViewName(templateName, request.getLocale());
		uiExposeHelper.addVariables(request);
		try {
			ResponseWrapper wrapper = new ResponseWrapper(response);
			view.render(model, request, wrapper);
			commit();
			return wrapper.getRendered();
		} catch (Throwable e) {
			rollBack(e);
			throw UIExceptionUtils.convert(templateName, e);
		} finally {
			TransactionContext.remove();
		}
	}

	/**
	 * @see TransactionTemplate#execute(org.springframework.transaction.support.TransactionCallback)
	 */
	private void rollBack(Throwable ex) {
		TransactionStatus status = TransactionContext.get();
		if (status != null && !status.isCompleted()) {
			try {
				this.transactionManager.rollback(status);
			} catch (TransactionSystemException ex2) {
				logger.error("Application exception overridden by rollback exception", ex);
				ex2.initApplicationException(ex);
				throw ex2;
			} catch (RuntimeException ex2) {
				logger.error("Application exception overridden by rollback exception", ex);
				throw ex2;
			} catch (Error err) {
				logger.error("Application exception overridden by rollback error", ex);
				throw err;
			}
		}
	}

	private void commit() {
		TransactionStatus status = TransactionContext.get();
		if (status != null && !status.isCompleted()) {
			transactionManager.commit(status);
		}
	}

	private static final class ResponseWrapper extends HttpServletResponseWrapper {

		private FastStringWriter writer = new FastStringWriter(100);

		public ResponseWrapper(HttpServletResponse response) {
			super(response);
		}

		@Override
		public PrintWriter getWriter() throws IOException {
			return new PrintWriter(writer);
		}

		public String getRendered() {
			return writer.toString();
		}
	}

}
