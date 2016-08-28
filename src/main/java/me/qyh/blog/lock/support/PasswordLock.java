package me.qyh.blog.lock.support;

import javax.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import me.qyh.blog.lock.LockKey;
import me.qyh.blog.lock.LockKeyInputException;
import me.qyh.blog.message.Message;
import me.qyh.blog.security.BCrypts;
import me.qyh.util.Validators;

public class PasswordLock extends DefaultLock {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static final String PASSWORD_PARAMETER = "password";

	private String password;

	public PasswordLock() {
		super(LockType.PASSWORD);
	}

	@Override
	public LockKey getKeyFromRequest(HttpServletRequest request) throws LockKeyInputException {
		final String password = request.getParameter(PASSWORD_PARAMETER);
		if (Validators.isEmptyOrNull(password, true)) {
			throw new LockKeyInputException(new Message("lock.password.password.blank", "密码不能为空"));
		}
		return new LockKey() {

			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			public Object getKey() {
				return password;
			}
		};
	}

	@Override
	public boolean tryOpen(LockKey key) {
		if (key != null) {
			Object keyData = key.getKey();
			if (keyData != null && BCrypts.matches(keyData.toString(), password)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public String keyInputUrl() {
		return "/unlock/plock";
	}

	@JsonIgnore
	public String getPassword() {
		return password;
	}

	@JsonProperty
	public void setPassword(String password) {
		this.password = password;
	}

}
