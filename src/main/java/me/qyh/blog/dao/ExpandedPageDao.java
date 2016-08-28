package me.qyh.blog.dao;

import java.util.List;

import me.qyh.blog.ui.page.ExpandedPage;

public interface ExpandedPageDao {

	ExpandedPage selectById(Integer id);

	void insert(ExpandedPage page);

	void update(ExpandedPage page);
	
	List<ExpandedPage> selectAll();
	
	void deleteById(Integer id);

}
