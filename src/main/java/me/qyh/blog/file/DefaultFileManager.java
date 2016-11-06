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
	private Map<Integer, FileServer> serverMap = new HashMap<Integer, FileServer>();

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
