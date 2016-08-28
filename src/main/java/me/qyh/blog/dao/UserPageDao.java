package me.qyh.blog.dao;

import java.util.List;

import me.qyh.blog.pageparam.UserPageQueryParam;
import me.qyh.blog.ui.page.UserPage;

public interface UserPageDao {

	UserPage selectById(Integer id);

	void update(UserPage page);

	void insert(UserPage page);
	
	int selectCount(UserPageQueryParam param);
	
	List<UserPage> selectPage(UserPageQueryParam param);

	void deleteById(Integer id);

}
