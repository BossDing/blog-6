package me.qyh.blog.file.local;

import org.springframework.web.multipart.MultipartFile;

import me.qyh.blog.file.FileStore;

public interface LocalFileStore extends FileStore {
	/**
	 * 是否能够存储该文件(方便将不同资源的文件存储在不同的位置)
	 * 
	 * @param multipartFile
	 * @return
	 */
	boolean canStore(MultipartFile multipartFile);

}
