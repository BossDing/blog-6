package me.qyh.blog.dao;

import me.qyh.blog.entity.Comment;

public interface CommentDao {

	Comment selectById(Integer id);

	void insert(Comment comment);
	
	int selectPathCount(String path);
	
	void deleteByPath(String path);
	
	void deleteById(Integer id);

}
