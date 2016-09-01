package me.qyh.blog.oauth2;

import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import me.qyh.blog.config.Constants;
import me.qyh.blog.entity.OauthUser;
import me.qyh.blog.security.AuthencationException;

public class OauthUserArgumentResolver implements HandlerMethodArgumentResolver {

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return parameter.hasParameterAnnotation(RequestOauthUser.class);
	}

	@Override
	public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
		OauthUser user = (OauthUser) webRequest.getAttribute(Constants.OAUTH_SESSION_KEY,
				RequestAttributes.SCOPE_SESSION);
		if (user == null) {
			RequestOauthUser ann = parameter.getParameterAnnotation(RequestOauthUser.class);
			if (ann.throwAuthencationExceptionIfNotExists()) {
				throw new AuthencationException();
			}
		}
		return user;
	}

}
