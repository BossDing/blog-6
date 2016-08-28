package me.qyh.blog.security;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import me.qyh.blog.entity.User;

public interface RememberMe {

	void remove(HttpServletRequest request, HttpServletResponse response);

	void save(User user, HttpServletRequest request, HttpServletResponse response);

	User login(HttpServletRequest request, HttpServletResponse response);

}
