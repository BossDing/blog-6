package me.qyh.blog.dao;

import me.qyh.blog.entity.User;

public interface UserDao {

	public User selectByName(String name);

	public void update(User current);

	public User selectById(Integer id);

}
