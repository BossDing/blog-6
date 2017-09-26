package me.qyh.blog.web.file;

import org.springframework.beans.BeansException;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;

public class FileStoreUrlHandlerMapping extends SimpleUrlHandlerMapping {

	public void registerFileStoreMapping(String urlPath, LocalResourceRequestHandlerFileStore fileStore)
			throws BeansException, IllegalStateException {
		super.registerHandler(urlPath, fileStore);
	}

}