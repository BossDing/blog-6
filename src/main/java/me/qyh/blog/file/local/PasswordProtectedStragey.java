package me.qyh.blog.file.local;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.web.util.UriComponentsBuilder;

import me.qyh.blog.exception.SystemException;
import me.qyh.util.Validators;

public class PasswordProtectedStragey implements ProtectedStragey, InitializingBean {

	private static final String AUTH_PARAMETER = "auth";

	public PasswordProtectedStragey(String password) {
		this.password = password;
	}

	private String password;

	@Override
	public boolean match(HttpServletRequest request) {
		String auth = request.getParameter(AUTH_PARAMETER);
		if (Validators.isEmptyOrNull(auth, true)) {
			return false;
		}
		return auth.equals(password);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (Validators.isEmptyOrNull(password, true)) {
			throw new SystemException("保护密码不能为空");
		}
	}

	@Override
	public String getAuthencatedUrl(String defaultVisitUrl) {
		UriComponentsBuilder ucb = UriComponentsBuilder.fromHttpUrl(defaultVisitUrl);
		ucb.queryParam(AUTH_PARAMETER, password);
		return ucb.build().toUriString();
	}

}
