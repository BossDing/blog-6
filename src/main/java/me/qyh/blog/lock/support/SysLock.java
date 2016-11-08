/*
 * Copyright 2016 qyh.me
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
