package me.qyh.blog.lock;

import java.io.Serializable;

import javax.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import me.qyh.blog.input.JsonHtmlXssSerializer;

/**
 * 锁，如果用户为对象设置了锁，那么访问的时候需要解锁才能访问(非登录用户)
 * 
 * @author Administrator
 *
 */
public abstract class Lock implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String id;
	@JsonSerialize(using = JsonHtmlXssSerializer.class)
	private String name;

	/**
	 * 从请求中获取钥匙
	 * 
	 * @param request
	 * @throws LockException
	 */
	public abstract LockKey getKeyFromRequest(HttpServletRequest request) throws InvalidKeyException;

	/**
	 * 开锁
	 * 
	 * @return
	 */
	public abstract void tryOpen(LockKey key) throws ErrorKeyException;

	/**
	 * 获取锁类型(用于模板)
	 * 
	 * @return
	 */
	public abstract String getLockType();

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
