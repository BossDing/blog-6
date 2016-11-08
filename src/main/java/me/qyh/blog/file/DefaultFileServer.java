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
package me.qyh.blog.file;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import me.qyh.blog.exception.LogicException;
import me.qyh.blog.exception.SystemException;

public class DefaultFileServer<T extends FileStore> implements FileServer, InitializingBean {

	private int id;
	private String name;
	protected List<T> stores;// 注意顺序
	private Map<Integer, T> storeMap = new HashMap<Integer, T>();

	@Override
	public CommonFile store(String key, MultipartFile file) throws LogicException, IOException {
		return stores.get(0).store(key, file);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (CollectionUtils.isEmpty(stores)) {
			throw new SystemException("请至少配置一个文件存储器");
		}

		for (T store : stores) {
			if (storeMap.containsKey(store.id())) {
				throw new SystemException("已经包含ID为" + store.id() + "的存储器了");
			}
			storeMap.put(store.id(), store);
		}
	}

	@Override
	public int id() {
		return id;
	}

	@Override
	public FileStore getFileStore(int id) {
		return storeMap.get(id);
	}

	@Override
	public String name() {
		return name;
	}

	@Override
	public List<FileStore> allStore() {
		List<FileStore> stores = new ArrayList<>();
		for (T t : this.stores)
			stores.add(t);
		return Collections.unmodifiableList(stores);
	}

	public void setId(int id) {
		this.id = id;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setStores(List<T> stores) {
		this.stores = stores;
	}

}
