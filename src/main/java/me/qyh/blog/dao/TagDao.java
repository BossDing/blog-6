package me.qyh.blog.dao;

import java.util.List;

import me.qyh.blog.entity.Tag;
import me.qyh.blog.pageparam.TagQueryParam;

public interface TagDao {

	void insert(Tag tag);

	Tag selectByName(String name);

	int selectCount(TagQueryParam param);

	List<Tag> selectPage(TagQueryParam param);
	
	void update(Tag tag);
	
	List<Tag> selectAll();

	Tag selectById(Integer id);
	
	void deleteById(Integer id);
}
