package me.qyh.blog.file.local;

import java.io.File;

import javax.servlet.http.HttpServletRequest;

import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public class CommonResourceStore extends AbstractLocalResourceRequestHandlerFileStore {

	@Override
	public boolean canStore(MultipartFile multipartFile) {
		return true;
	}

	@Override
	protected Resource getResource(String path, HttpServletRequest request) {
		File file = getFile(path);
		return file == null ? null : new PathResource(file.toPath());
	}

}
