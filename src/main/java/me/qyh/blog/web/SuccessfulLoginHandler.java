package me.qyh.blog.web;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import me.qyh.blog.core.entity.User;

public interface SuccessfulLoginHandler {

	void afterSuccessfulLogin(User user, HttpServletRequest request, HttpServletResponse response);

}