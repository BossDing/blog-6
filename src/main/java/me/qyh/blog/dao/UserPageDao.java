package me.qyh.blog.dao;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import me.qyh.blog.entity.Space;
import me.qyh.blog.pageparam.UserPageQueryParam;
import me.qyh.blog.ui.page.UserPage;

public interface UserPageDao {

	UserPage selectById(Integer id);

	void update(UserPage page);

	void insert(UserPage page);
	
	int selectCount(UserPageQueryParam param);
	
	List<UserPage> selectPage(UserPageQueryParam param);

	void deleteById(Integer id);

	UserPage selectByAlias(String alias);

	List<UserPage> selectBySpace(@Param("space") Space space);

}
