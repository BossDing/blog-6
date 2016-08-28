package me.qyh.blog.lock;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.Validator;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * 用于paramter的annotation，被标记后将会返回一个Lock对象(<strong>不会为null</strong>),
 * 如果LockParser提供了Validators，将会利用这些Validators进行验证，如果验证不通过，将会抛出
 * MethodArgumentNotValidException异常
 * 
 * @author Administrator
 * @see LockParser
 */
public class LockArgumentResolver implements HandlerMethodArgumentResolver {

	@Autowired
	private LockParser<?> lockParser;

	private static final String LOCK_NAME = "lock";

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return parameter.hasParameterAnnotation(RequestLock.class);
	}

	@Override
	public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
		final HttpServletRequest servletRequest = webRequest.getNativeRequest(HttpServletRequest.class);
		Lock lock = null;
		try {
			lock = lockParser.getLockFromRequest(servletRequest);
		} catch (Exception e) {
			throw new HttpMessageNotReadableException(e.getMessage(), e);
		}
		if (lock == null) {
			throw new HttpMessageNotReadableException("无法从请求中解析锁");
		}
		// 做验证
		WebDataBinder binder = binderFactory.createBinder(webRequest, lock, LOCK_NAME);
		Validator[] validators = lockParser.getValidators();
		if (validators != null && validators.length > 0) {
			binder.addValidators(validators);
			binder.validate();
			BindingResult bindingResult = binder.getBindingResult();
			if (bindingResult.hasErrors()) {
				throw new MethodArgumentNotValidException(parameter, bindingResult);
			}
		}
		mavContainer.addAttribute(BindingResult.MODEL_KEY_PREFIX + LOCK_NAME, binder.getBindingResult());
		return lock;
	}

}
