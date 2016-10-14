package me.qyh.blog.dao;

import java.util.List;

import me.qyh.blog.entity.FileDelete;

public interface FileDeleteDao {

	void insert(FileDelete fileDelete);

	List<FileDelete> selectAll();

	void deleteById(Integer id);

	List<FileDelete> selectChildren(String key);
	
	void deleteChildren(String key);

}
