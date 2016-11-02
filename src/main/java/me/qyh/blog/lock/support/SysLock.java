package me.qyh.blog.lock.support;

import java.sql.Timestamp;

import javax.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import me.qyh.blog.exception.SystemException;
import me.qyh.blog.lock.ErrorKeyException;
import me.qyh.blog.lock.Lock;
import me.qyh.blog.lock.LockKey;
import me.qyh.blog.lock.InvalidKeyException;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SysLock extends Lock {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private SysLockType type;
	private Timestamp createDate;

	public SysLock() {

	}

	protected SysLock(SysLockType type) {
		this.type = type;
	}

	/**
	 * 锁类型
	 * 
	 * @author Administrator
	 *
	 */
	public enum SysLockType {
		PASSWORD, // 密码锁
		QA// 问答锁
	}

	public SysLockType getType() {
		return type;
	}

	@Override
	public LockKey getKeyFromRequest(HttpServletRequest request) throws InvalidKeyException {
		throw new SystemException("不支持的操作");
	}

	@Override
	public void tryOpen(LockKey key) throws ErrorKeyException {
		throw new SystemException("不支持的操作");
	}

	public Timestamp getCreateDate() {
		return createDate;
	}

	public void setCreateDate(Timestamp createDate) {
		this.createDate = createDate;
	}

	@Override
	public String getLockType() {
		return type.name();
	}
}
