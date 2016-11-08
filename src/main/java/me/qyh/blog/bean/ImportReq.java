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
package me.qyh.blog.bean;

import java.util.ArrayList;
import java.util.List;

import me.qyh.blog.entity.Space;

public class ImportReq {

	private boolean insertNotExistsFragment;
	private boolean updateExistsFragment;
	private Space space;
	private List<Integer> ids = new ArrayList<Integer>();

	public boolean isInsertNotExistsFragment() {
		return insertNotExistsFragment;
	}

	public void setInsertNotExistsFragment(boolean insertNotExistsFragment) {
		this.insertNotExistsFragment = insertNotExistsFragment;
	}

	public boolean isUpdateExistsFragment() {
		return updateExistsFragment;
	}

	public void setUpdateExistsFragment(boolean updateExistsFragment) {
		this.updateExistsFragment = updateExistsFragment;
	}

	public Space getSpace() {
		return space;
	}

	public void setSpace(Space space) {
		this.space = space;
	}

	public List<Integer> getIds() {
		return ids;
	}

	public void setIds(List<Integer> ids) {
		this.ids = ids;
	}

}
