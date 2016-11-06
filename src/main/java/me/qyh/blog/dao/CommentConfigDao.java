package me.qyh.blog.dao;

import me.qyh.blog.entity.CommentConfig;

public interface CommentConfigDao {

	void deleteById(Integer id);

	void update(CommentConfig config);

	void insert(CommentConfig config);

}
