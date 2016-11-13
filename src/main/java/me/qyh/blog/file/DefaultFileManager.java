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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.CollectionUtils;

import me.qyh.blog.exception.SystemException;

public class DefaultFileManager implements FileManager, InitializingBean {

	private List<FileServer> servers;
	private Map<Integer, FileServer> serverMap = new HashMap<>();

	public void setServers(List<FileServer> servers) {
		this.servers = servers;
	}

	@Override
	public FileServer getFileServer(int id) {
		return serverMap.get(id);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (CollectionUtils.isEmpty(servers)) {
			throw new SystemException("文件服务不能为空");
		}
		for (FileServer server : servers) {
			serverMap.put(server.id(), server);
		}
	}

	@Override
	public List<FileServer> getAllServers() {
		return Collections.unmodifiableList(servers);
	}

	@Override
	public FileServer getFileServer() {
		return servers.get(0);
	}
}
