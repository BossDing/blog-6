package me.qyh.blog.file.local;

import me.qyh.blog.file.Resize;

/**
 * 缩略图请求解析器
 * 
 * @author mhlx
 *
 */
public interface ResizeUrlParser {

	/**
	 * 从缩略图请求中获取缩放信息
	 * 
	 * @param path
	 * @return
	 */
	Resize getResizeFromPath(String path);

	/**
	 * 根据缩放信息获取缩略图链接
	 * 
	 * @param resize
	 * @param path
	 * @return
	 */
	String generateResizePathFromPath(Resize resize, String path);

	/**
	 * 从缩略图链接中获取原图链接
	 * 
	 * @param path
	 * @return
	 */
	String getSourcePathByResizePath(String path);

	/**
	 * 是否是可被接受的缩放
	 * 
	 * @param resize
	 * @return
	 */
	boolean validResize(Resize resize);

}
