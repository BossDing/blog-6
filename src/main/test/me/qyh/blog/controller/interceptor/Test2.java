package me.qyh.blog.controller.interceptor;

import org.springframework.web.multipart.MultipartFile;

import me.qyh.blog.file.CommonFile;

public interface Test2 {

	/**
	 * 删除文件
	 * 
	 * @param cf
	 */
	public void delete(CommonFile cf);

	/**
	 * 存储文件
	 * 
	 * @param multipartFile
	 */
	public CommonFile upload(MultipartFile multipartFile);
}
