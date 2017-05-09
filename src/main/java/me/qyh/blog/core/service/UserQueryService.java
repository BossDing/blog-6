package me.qyh.blog.core.service;

import me.qyh.blog.core.entity.User;

public interface UserQueryService {

	/**
	 * 获取管理员信息
	 * 
	 * @return
	 */
	User getUser();

}
