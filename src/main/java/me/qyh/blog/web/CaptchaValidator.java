package me.qyh.blog.web;

import javax.servlet.http.HttpServletRequest;

import me.qyh.blog.core.exception.LogicException;

public interface CaptchaValidator {

	void doValidate(HttpServletRequest request) throws LogicException;

}
