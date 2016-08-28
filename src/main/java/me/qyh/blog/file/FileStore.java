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
	CommonFile store(MultipartFile multipartFile) throws LogicException, IOException;

	/**
	 * 存储器ID
	 * 
	 * @return
	 */
	int id();

	/**
	 * 删除物理文件
	 * 
	 * @param t
	 * @return true:删除成功|文件不存在，无需删除
	 * 		false :删除失败(可能占用中)
 	 */
	boolean delete(CommonFile t);

	/**
	 * 获取文件的访问路径
	 * 
	 * @param cf
	 * @return
	 */
	String getUrl(CommonFile cf);

	/**
	 * 获取下载路径
	 * 
	 * @param cf
	 * @return
	 */
	String getDownloadUrl(CommonFile cf);

	/**
	 * 获取预览路径
	 * 
	 * @param cf
	 * @return
	 */
	String getPreviewUrl(CommonFile cf);

}
