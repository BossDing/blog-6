package me.qyh.blog.file.local;

import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;

public class LocalResourceSimpleUrlHandlerMapping extends SimpleUrlHandlerMapping {

	public LocalResourceSimpleUrlHandlerMapping() {
		setUrlMap(LocalResourceUrlMappingHolder.get());
	}
}
