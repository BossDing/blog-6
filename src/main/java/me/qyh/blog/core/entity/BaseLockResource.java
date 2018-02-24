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
package me.qyh.blog.core.entity;

import java.util.Optional;

/**
 * 
 * @author Administrator
 *
 */
public abstract class BaseLockResource extends BaseEntity implements LockResource {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private String lockId;

	/**
	 * default
	 */
	BaseLockResource() {
		super();
	}

	/**
	 * 
	 * @param id
	 *            资源id
	 */
	BaseLockResource(Integer id) {
		super(id);
	}

	@Override
	public String getResource() {
		return this.getClass().getSimpleName() + ":" + getId();
	}

	@Override
	public final Optional<String> getLock() {
		return lockId == null ? Optional.empty() : Optional.of(lockId);
	}

	public void setLockId(String lockId) {
		this.lockId = lockId;
	}

	public String getLockId() {
		return lockId;
	}

	public boolean hasLock() {
		return getLock().isPresent();
	}
}
