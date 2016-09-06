package me.qyh.blog.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import me.qyh.blog.dao.UserDao;
import me.qyh.blog.entity.User;
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.security.BCrypts;
import me.qyh.blog.service.UserService;
import me.qyh.blog.web.controller.form.LoginBean;
import me.qyh.util.Validators;

@Service
@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
public class UserServiceImpl implements UserService {

	@Autowired
	private UserDao userDao;

	@Override
	public void updateUser(User user) throws LogicException {
		if (!Validators.isEmptyOrNull(user.getPassword(), true)) {
			user.setPassword(BCrypts.encode(user.getPassword()));
		}
		userDao.update(user);
	}

	@Override
	@Transactional(readOnly = true)
	public User login(LoginBean loginBean) throws LogicException {
		User user = userDao.select();
		if (user != null) {
			if (user.getName().equals(loginBean.getUsername())) {
				String encrptPwd = user.getPassword();
				if (BCrypts.matches(loginBean.getPassword(), encrptPwd)) {
					return user;
				}
			}
		}
		throw new LogicException("user.loginFail", "登录失败");
	}

	@Override
	@Transactional(readOnly = true)
	public User select() {
		return userDao.select();
	}
}
