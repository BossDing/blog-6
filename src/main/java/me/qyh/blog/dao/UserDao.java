package me.qyh.blog.dao;

import me.qyh.blog.entity.User;

public interface UserDao {

	public User select();

	public void update(User current);

}
