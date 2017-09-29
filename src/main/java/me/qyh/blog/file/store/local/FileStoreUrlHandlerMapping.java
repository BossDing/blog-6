package me.qyh.blog.file.store.local;

import org.springframework.beans.BeansException;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;

/**
 * UrlMapping，用來注册本地文件存储器
 * @date 2017年9月29日 上午10:34:44
 * @author 钱宇豪
 */
public class FileStoreUrlHandlerMapping extends SimpleUrlHandlerMapping {

	public void registerFileStoreMapping(String urlPath, LocalResourceRequestHandlerFileStore fileStore)
			throws BeansException, IllegalStateException {
		super.registerHandler(urlPath, fileStore);
	}

}