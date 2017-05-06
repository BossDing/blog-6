package me.qyh.blog.core.service.impl;

import org.springframework.stereotype.Service;

import me.qyh.blog.core.config.UserConfig;
import me.qyh.blog.core.entity.User;
import me.qyh.blog.core.exception.LogicException;
import me.qyh.blog.core.security.BCrypts;
import me.qyh.blog.core.service.UserService;
import me.qyh.blog.web.controller.form.LoginBean;

@Service
public class UserServiceImpl implements UserService {

	@Override
	public User login(LoginBean loginBean) throws LogicException {
		User user = UserConfig.get();
		if (user.getName().equals(loginBean.getUsername())) {
			verifyPassword(loginBean.getPassword());
			return user;
		}
		throw new LogicException("user.loginFail", "登录失败");
	}

	private void verifyPassword(String password) throws LogicException {
		if (!BCrypts.matches(password, UserConfig.get().getPassword())) {
			throw new LogicException("user.password.verifyFail", "密码校验失败");
		}
	}

	@Override
	public void update(User user, String password) throws LogicException {
		verifyPassword(password);
		UserConfig.update(user);
	}
}
