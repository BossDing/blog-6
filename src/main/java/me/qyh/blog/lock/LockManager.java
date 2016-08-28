package me.qyh.blog.lock;

import java.util.List;

import me.qyh.blog.exception.LogicException;

public interface LockManager<T extends Lock> {
	
	/**
	 * 根据ID获取锁
	 * 
	 * @param id
	 *            锁的ID
	 * @return null如果不存在
	 */
	T findLock(String id);

	/**
	 * 获取所有的锁
	 * 
	 * @param param
	 * @return
	 */
	List<T> allLock();

	/**
	 * 增加锁
	 */
	void addLock(T lock) throws LogicException;

	/**
	 * 删除锁
	 * 
	 * @param id
	 */
	void removeLock(String id) throws LogicException;

	/**
	 * 更新锁
	 * 
	 * @param lock
	 */
	void updateLock(T lock) throws LogicException;

	/**
	 * 管理页面
	 * 
	 * @return
	 */
	String managePage();

}
