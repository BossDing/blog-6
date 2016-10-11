package me.qyh.blog.dao;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import me.qyh.blog.entity.Space;
import me.qyh.blog.pageparam.UserFragmentQueryParam;
import me.qyh.blog.ui.fragment.UserFragment;

public interface UserFragmentDao {

	void insert(UserFragment userFragment);

	void deleteById(Integer id);

	List<UserFragment> selectPage(UserFragmentQueryParam param);

	int selectCount(UserFragmentQueryParam param);

	void update(UserFragment userFragment);

	UserFragment selectBySpaceAndName(@Param("space") Space space, @Param("name") String name);

	UserFragment selectById(Integer id);

	UserFragment selectGlobalByName(String name);

}
