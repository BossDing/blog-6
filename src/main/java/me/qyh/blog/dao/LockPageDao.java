package me.qyh.blog.dao;

import org.apache.ibatis.annotations.Param;

import me.qyh.blog.entity.Space;
import me.qyh.blog.ui.page.LockPage;

public interface LockPageDao {

	LockPage selectBySpaceAndLockType(@Param("space") Space space, @Param("lockType") String lockType);

	void insert(LockPage lockPage);

	void update(LockPage lockPage);

	void deleteById(Integer id);

	LockPage selectById(Integer id);

}
