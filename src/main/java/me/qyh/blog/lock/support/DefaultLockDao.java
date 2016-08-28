package me.qyh.blog.lock.support;

import java.util.List;

public interface DefaultLockDao {

	List<DefaultLock> selectAll();

	void delete(String id);

	void insert(DefaultLock lock);

	void update(DefaultLock lock);

	DefaultLock selectById(String id);

}
