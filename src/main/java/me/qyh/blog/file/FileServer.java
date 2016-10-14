package me.qyh.blog.file;

import java.io.IOException;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import me.qyh.blog.exception.LogicException;

public interface FileServer {

	/**
	 * 储存文件
	 * 
	 * @param file
	 *            上传的文件
	 * @return
	 * @throws LogicException
	 * @throws IOException
	 */
	CommonFile store(String key, MultipartFile file) throws LogicException, IOException;

	int id();

	FileStore getFileStore(int id);

	String name();
	
	List<FileStore> allStore();

}
