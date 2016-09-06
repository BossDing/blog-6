package me.qyh.blog.service;

import me.qyh.blog.entity.User;
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.web.controller.form.LoginBean;

public interface UserService {

	/**
	 * 更新用户
	 * 
	 * @param oldPwd
	 * @param newPwd
	 * @throws LogicException
	 */
	void updateUser(User user) throws LogicException;

	/**
	 * 登录
	 * 
	 * @param username
	 *            用户名
	 * @param password
	 *            密码
	 * @return 登录成功后的用户
	 * @throws LogicException
	 *             登录失败
	 */
	User login(LoginBean loginBean) throws LogicException;

	User select();

}
