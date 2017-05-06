package me.qyh.blog.core.service;

import me.qyh.blog.core.entity.User;
import me.qyh.blog.core.exception.LogicException;
import me.qyh.blog.web.controller.form.LoginBean;

public interface UserService {

	User login(LoginBean loginBean) throws LogicException;

	void update(User user, String password) throws LogicException;

}
