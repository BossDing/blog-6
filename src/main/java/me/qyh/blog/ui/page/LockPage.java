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
package me.qyh.blog.ui.page;

import me.qyh.blog.entity.Space;

public class LockPage extends Page {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String lockType;

	public LockPage() {
		super();
	}

	public LockPage(Space space) {
		super(space);
	}

	public LockPage(Space space, String lockType) {
		super(space);
		this.lockType = lockType;
	}

	public LockPage(String lockType) {
		super();
		this.lockType = lockType;
	}

	public LockPage(LockPage page) {
		super(page);
		this.lockType = page.lockType;
	}

	public String getLockType() {
		return lockType;
	}

	public void setLockType(String lockType) {
		this.lockType = lockType;
	}

	@Override
	public final PageType getType() {
		return PageType.LOCK;
	}

	public Page toExportPage() {
		LockPage page = new LockPage();
		page.setTpl(getTpl());
		page.setType(PageType.LOCK);
		page.setLockType(lockType);
		return page;
	}

}
