package me.qyh.blog.lock.support;

import javax.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import me.qyh.blog.exception.SystemException;
import me.qyh.blog.lock.Lock;
import me.qyh.blog.lock.LockKey;
import me.qyh.blog.lock.LockKeyInputException;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DefaultLock extends Lock {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private LockType type;

	public DefaultLock() {

	}

	protected DefaultLock(LockType type) {
		this.type = type;
	}

	/**
	 * 锁类型
	 * 
	 * @author Administrator
	 *
	 */
	public enum LockType {
		PASSWORD, // 密码锁
		QA// 问答锁
	}

	public LockType getType() {
		return type;
	}

	@Override
	public LockKey getKeyFromRequest(HttpServletRequest request) throws LockKeyInputException {
		throw new SystemException("不支持的操作");
	}

	@Override
	public boolean tryOpen(LockKey key) {
		throw new SystemException("不支持的操作");
	}

	@Override
	public String keyInputUrl() {
		throw new SystemException("不支持的操作");
	}

}
