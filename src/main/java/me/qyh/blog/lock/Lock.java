package me.qyh.blog.lock;

import java.io.Serializable;
import java.util.Date;

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
	private Date createDate;
	private LockResource lockResource;

	/**
	 * 从请求中获取钥匙
	 * 
	 * @param request
	 * @throws LockException
	 */
	public abstract LockKey getKeyFromRequest(HttpServletRequest request) throws LockKeyInputException;

	/**
	 * 开锁
	 * 
	 * @return
	 */
	public abstract boolean tryOpen(LockKey key);

	/**
	 * 解锁地址
	 * 
	 * @return
	 * @throws LockException
	 */
	public abstract String keyInputUrl();

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

	public LockResource getLockResource() {
		return lockResource;
	}

	public void setLockResource(LockResource lockResource) {
		this.lockResource = lockResource;
	}

	public Date getCreateDate() {
		return createDate;
	}

	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}

}
