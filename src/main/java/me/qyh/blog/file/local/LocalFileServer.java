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
package me.qyh.blog.file.local;

import org.springframework.web.multipart.MultipartFile;

import me.qyh.blog.exception.LogicException;
import me.qyh.blog.exception.SystemException;
import me.qyh.blog.file.CommonFile;
import me.qyh.blog.file.DefaultFileServer;

/**
 * 本地文件存储服务
 * 
 * @author Administrator
 *
 */
public class LocalFileServer extends DefaultFileServer<LocalFileStore> {

	@Override
	public CommonFile store(String key, MultipartFile file) throws LogicException {
		for (LocalFileStore store : stores) {
			if (store.canStore(file)) {
				return store.store(key, file);
			}
		}
		throw new SystemException("储存失败:" + file + "，没有找到符合条件的存储器");
	}
}
