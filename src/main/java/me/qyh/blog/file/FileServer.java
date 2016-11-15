/*
 * Copyright 2016 qyh.me
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package me.qyh.blog.file;

import java.io.IOException;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import me.qyh.blog.exception.LogicException;

/**
 * 
 * @author Administrator
 *
 */
public interface FileServer {

	/**
	 * 储存文件
	 * 
	 * @param key
	 *            文件路径
	 * @param file
	 *            文件
	 * @return 尺寸成功后的文件信息
	 * @throws LogicException
	 * @throws IOException
	 */
	CommonFile store(String key, MultipartFile file) throws LogicException;

	/**
	 * 存储服务id
	 * 
	 * @return
	 */
	int id();

	/**
	 * 根据id查询存储器
	 * 
	 * @param id
	 *            存储器id
	 * @return 如果不存在返回null
	 */
	FileStore getFileStore(int id);

	/**
	 * 
	 * @return 存储服务name
	 */
	String name();

	/**
	 * 获取所有的文件存储器
	 * 
	 * @return
	 */
	List<FileStore> allStore();

}
