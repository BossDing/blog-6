package me.qyh.blog.lock.support;

import java.util.List;

public interface SysLockDao {

	List<SysLock> selectAll();

	void delete(String id);

	void insert(SysLock lock);

	void update(SysLock lock);

	SysLock selectById(String id);

}
