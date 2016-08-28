package me.qyh.blog.dao;

import java.util.List;

import me.qyh.blog.file.CommonFile;

public interface CommonFileDao {

	void deleteById(Integer id);

	void insert(CommonFile commonFile);

	List<CommonFile> selectDeleted();

}
