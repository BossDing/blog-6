package me.qyh.blog.dao;

import me.qyh.blog.file.CommonFile;

public interface CommonFileDao {

	void deleteById(Integer id);

	void insert(CommonFile commonFile);
}
