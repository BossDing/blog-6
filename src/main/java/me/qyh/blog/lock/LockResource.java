package me.qyh.blog.lock;

import com.fasterxml.jackson.annotation.JsonIgnore;

public interface LockResource {

	/**
	 * 被锁保护的资源，应该提供一个唯一的ID
	 * 
	 * @return
	 */
	@JsonIgnore
	String getResourceId();

	/**
	 * 获取锁ID
	 * 
	 * @return
	 */
	String getLockId();

}
