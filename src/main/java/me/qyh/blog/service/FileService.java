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
package me.qyh.blog.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.web.multipart.MultipartFile;

import me.qyh.blog.bean.BlogFilePageResult;
import me.qyh.blog.bean.UploadedFile;
import me.qyh.blog.entity.BlogFile;
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.file.FileStore;
import me.qyh.blog.pageparam.BlogFileQueryParam;
import me.qyh.blog.pageparam.PageResult;
import me.qyh.blog.util.Validators;
import me.qyh.blog.web.controller.form.BlogFileUpload;

/**
 * 
 * @author Administrator
 *
 */
public interface FileService {

	public static final String SPLIT_CHAR = "/";

	/**
	 * 上传文件
	 * 
	 * @param upload
	 *            文件对象
	 * @return 上传详情
	 * @throws LogicException
	 *             上传过程中发生逻辑异常
	 */
	List<UploadedFile> upload(BlogFileUpload upload) throws LogicException;

	/**
	 * 创建文件夹
	 * 
	 * @param toCreate
	 *            待创建的文件夹
	 * @throws LogicException
	 *             创建逻辑异常
	 */
	void createFolder(BlogFile toCreate) throws LogicException;

	/**
	 * 分页查询文件
	 * 
	 * @param param
	 *            查询参数
	 * @return 文件分页对象
	 * @throws LogicException
	 *             查询逻辑异常
	 */
	BlogFilePageResult queryBlogFiles(BlogFileQueryParam param) throws LogicException;

	/**
	 * 获取文件属性
	 * 
	 * @param id
	 *            文件id
	 * @return 文件属性map
	 * @throws LogicException
	 *             查询属性异常
	 */
	Map<String, Object> getBlogFileProperty(Integer id) throws LogicException;

	/**
	 * 获取可存储文件的文件储存器
	 * 
	 * @return 文件服务列表
	 */
	List<FileStore> allStorableStores();

	/**
	 * 更新文件
	 * 
	 * @param toUpdate
	 *            待更新的文件
	 * @throws LogicException
	 *             更新过程中逻辑异常
	 */
	void update(BlogFile toUpdate) throws LogicException;

	/**
	 * 删除文件树节点(不会删除实际的物理文件)
	 * 
	 * @see FileService#clearDeletedCommonFile()
	 * @param id
	 *            文件id
	 * @throws LogicException
	 *             删除过程中逻辑异常
	 */
	void delete(Integer id) throws LogicException;

	/**
	 * 删除待删除状态的文件<br>
	 * <strong>当前仅当物理文件删除成功后才执行删除记录操作</strong>
	 * 
	 * @see FileStore#delete(me.qyh.blog.file.CommonFile)
	 */
	void clearDeletedCommonFile();

	/**
	 * 保存metaweblog api 上传的文件
	 * 
	 * @param file
	 *            待保存的文件
	 * @return 上传完成后的信息
	 * @throws LogicException
	 *             保存过程中发生逻辑异常
	 */
	UploadedFile uploadMetaweblogFile(MultipartFile file) throws LogicException;

	/**
	 * 查询<b>文件</b>
	 * <p>
	 * 用于DataTag
	 * </p>
	 * 
	 * @param path
	 *            路径，指向一个文件夹，如果为null或空或为/，查询根目录
	 * @param extensions
	 *            后缀，如果为null或者为空，查询所有后缀
	 * @param page
	 *            当前页
	 * @return
	 */
	PageResult<BlogFile> queryFiles(String path, Set<String> extensions, int page);

	/**
	 * 根据ID查询文件
	 * <p>
	 * <b>返回的文件路径为全路径</b>
	 * </p>
	 * 
	 * @param id
	 *            文件id
	 */
	Optional<BlogFile> getFile(int id);

	static String cleanPath(String path) {
		if (FileService.SPLIT_CHAR.equals(path)) {
			return "";
		}
		String cleaned = Validators.cleanPath(path);
		if (cleaned.startsWith(FileService.SPLIT_CHAR)) {
			cleaned = cleaned.substring(1, cleaned.length());
		}
		return cleaned;
	}

}
