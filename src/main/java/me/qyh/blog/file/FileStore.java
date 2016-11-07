package me.qyh.blog.file;

import java.io.IOException;

import org.springframework.web.multipart.MultipartFile;

import me.qyh.blog.exception.LogicException;

public interface FileStore {

	/**
	 * 存储文件
	 * 
	 * @param multipartFile
	 * @return
	 * @throws LogicException
	 *             存储异常
	 */
	CommonFile store(String key, MultipartFile multipartFile) throws LogicException, IOException;

	/**
	 * 存储器ID
	 * 
	 * @return
	 */
	int id();

	/**
	 * 删除物理文件
	 * 
	 * @param key
	 * @return true:删除成功|文件不存在，无需删除 false :删除失败(可能占用中)
	 */
	boolean delete(String key);

	/**
	 * 删除文件夹下物理文件
	 * 
	 * @param key
	 * @return true:如果文件夹不存在或者全部文件删除成功
	 */
	boolean deleteBatch(String key);

	/**
	 * 获取文件的访问路径
	 * 
	 * @param key
	 * @return
	 */
	String getUrl(String key);

	/**
	 * 获取下载路径
	 * 
	 * @param cf
	 * @return
	 */
	String getDownloadUrl(String key);

	/**
	 * 获取缩略图路径
	 * 
	 * @param cf
	 * @return
	 */
	ThumbnailUrl getThumbnailUrl(String key);

}
