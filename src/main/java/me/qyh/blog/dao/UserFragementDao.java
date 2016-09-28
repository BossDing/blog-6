package me.qyh.blog.dao;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import me.qyh.blog.entity.Space;
import me.qyh.blog.pageparam.UserFragementQueryParam;
import me.qyh.blog.ui.fragement.UserFragement;

public interface UserFragementDao {

	void insert(UserFragement userFragement);

	void deleteById(Integer id);

	List<UserFragement> selectPage(UserFragementQueryParam param);

	int selectCount(UserFragementQueryParam param);

	void update(UserFragement userFragement);

	UserFragement selectBySpaceAndName(@Param("space") Space space, @Param("name") String name);

	UserFragement selectById(Integer id);

	UserFragement selectGlobalByName(String name);

}
