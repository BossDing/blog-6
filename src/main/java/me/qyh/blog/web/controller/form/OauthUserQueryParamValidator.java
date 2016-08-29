package me.qyh.blog.web.controller.form;

import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import me.qyh.blog.pageparam.OauthUserQueryParam;

@Component
public class OauthUserQueryParamValidator implements Validator {

	private static final int MAX_NICKNAME_LENGTH = 20;

	@Override
	public boolean supports(Class<?> clazz) {
		return OauthUserQueryParam.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		OauthUserQueryParam param = (OauthUserQueryParam) target;
		String nickname = param.getNickname();
		if (nickname != null && nickname.length() > MAX_NICKNAME_LENGTH) {
			param.setNickname(nickname.substring(0, MAX_NICKNAME_LENGTH));
		}
		if (param.getCurrentPage() < 1) {
			param.setCurrentPage(1);
		}
	}

}
