package me.qyh.blog.lock;

import java.io.InputStream;

import javax.servlet.http.HttpServletRequest;

import org.springframework.core.MethodParameter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectReader;

import me.qyh.blog.lock.support.PasswordLock;
import me.qyh.blog.lock.support.QALock;
import me.qyh.blog.lock.support.SysLock;
import me.qyh.util.Jsons;
import me.qyh.util.Validators;

public class LockArgumentResolver implements HandlerMethodArgumentResolver {

	private SysLockValidator validator = new SysLockValidator();

	private static final String LOCK_NAME = "lock";

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return parameter.hasParameterAnnotation(RequestLock.class);
	}

	@Override
	public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
		final HttpServletRequest servletRequest = webRequest.getNativeRequest(HttpServletRequest.class);
		SysLock lock = null;
		try {
			lock = getLockFromRequest(servletRequest);
		} catch (Exception e) {
			throw new HttpMessageNotReadableException(e.getMessage(), e);
		}
		if (lock == null) {
			throw new HttpMessageNotReadableException("无法从请求中解析锁");
		}
		// 做验证
		WebDataBinder binder = binderFactory.createBinder(webRequest, lock, LOCK_NAME);
		binder.setValidator(validator);
		binder.validate();
		BindingResult bindingResult = binder.getBindingResult();
		if (bindingResult.hasErrors()) {
			throw new MethodArgumentNotValidException(parameter, bindingResult);
		}
		mavContainer.addAttribute(BindingResult.MODEL_KEY_PREFIX + LOCK_NAME, binder.getBindingResult());
		return lock;
	}

	private SysLock getLockFromRequest(HttpServletRequest request) throws Exception {
		InputStream is = request.getInputStream();
		ObjectReader reader = Jsons.reader();
		JsonParser jp = reader.getFactory().createParser(is);
		JsonNode node = Jsons.reader().readTree(jp);
		JsonNode typeNode = node.get("type");
		if (typeNode != null && typeNode.textValue() != null) {
			String type = typeNode.textValue();
			if (type != null) {
				switch (type) {
				case "PASSWORD":
					return reader.treeToValue(node, PasswordLock.class);
				case "QA":
					return reader.treeToValue(node, QALock.class);
				}
			}
		}
		return null;
	}

	private final class SysLockValidator implements Validator {

		private static final int MAX_NAME_LENGTH = 20;
		private static final int MAX_PASSWORD_LENGTH = 16;
		private static final int MAX_QUESTION_LENGTH = 10000;
		private static final int MAX_ANSWERS_LENGTH = 500;
		private static final int MAX_ANSWERS_SIZE = 10;// 答案的个数

		@Override
		public boolean supports(Class<?> clazz) {
			return SysLock.class.isAssignableFrom(clazz);
		}

		@Override
		public void validate(Object target, Errors errors) {
			SysLock lock = (SysLock) target;
			String name = lock.getName();
			if (Validators.isEmptyOrNull(name, true)) {
				errors.reject("lock.name.empty", "锁的名称不能为空");
				return;
			}
			if (name.length() > MAX_NAME_LENGTH) {
				errors.reject("lock.name.toolong", "锁的名称不能超过" + MAX_NAME_LENGTH + "个字符");
				return;
			}
			switch (lock.getType()) {
			case PASSWORD:
				PasswordLock plock = (PasswordLock) lock;
				String password = plock.getPassword();
				if (Validators.isEmptyOrNull(password, true)) {
					errors.reject("lock.pwd.empty", "锁的密码不能为空");
					return;
				}
				if (password.length() > MAX_PASSWORD_LENGTH) {
					errors.reject("lock.pwd.toolong", "锁的密码不能超过" + MAX_PASSWORD_LENGTH + "个字符");
					return;
				}
				break;
			case QA:
				QALock qaLock = (QALock) lock;
				String question = qaLock.getQuestion();
				if (Validators.isEmptyOrNull(question, true)) {
					errors.reject("lock.question.empty", "问题不能为空");
					return;
				}
				if (question.length() > MAX_QUESTION_LENGTH) {
					errors.reject("lock.question.toolong", "问题不能超过" + MAX_QUESTION_LENGTH + "个字符");
					return;
				}

				String answers = qaLock.getAnswers();
				if (answers == null || answers.isEmpty()) {
					errors.reject("lock.answers.empty", "答案不能为空");
					return;
				}
				if (answers.length() > MAX_ANSWERS_LENGTH) {
					errors.reject("lock.answers.toolong", "答案不能超过" + MAX_ANSWERS_LENGTH + "个字符");
					return;
				}

				String[] _answers = answers.split(",");
				if (_answers.length > MAX_ANSWERS_SIZE) {
					errors.reject("lock.answers.oversize", "答案不能超过" + MAX_ANSWERS_SIZE + "个");
					return;
				}
			}
		}

	}
}
