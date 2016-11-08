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

import org.springframework.web.multipart.MultipartFile;

import me.qyh.blog.bean.BlogFilePageResult;
import me.qyh.blog.bean.UploadedFile;
import me.qyh.blog.entity.BlogFile;
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.file.FileServer;
import me.qyh.blog.file.FileStore;
import me.qyh.blog.pageparam.BlogFileQueryParam;
import me.qyh.blog.web.controller.form.BlogFileUpload;

public interface FileService {

	public static final String SPLIT_CHAR = "/";

	List<UploadedFile> upload(BlogFileUpload upload) throws LogicException;

	void createFolder(BlogFile toCreate) throws LogicException;

	BlogFilePageResult queryBlogFiles(BlogFileQueryParam param) throws LogicException;

	Map<String, Object> getBlogFileProperty(Integer id) throws LogicException;

	List<FileServer> allServers();

	void update(BlogFile toUpdate) throws LogicException;

	/**
	 * 删除文件树节点(不会删除实际的物理文件)
	 * 
	 * @see FileService#clearDeletedCommonFile()
	 * @param id
	 * @throws LogicException
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
	 * 上传metaweblog api上传的文件
	 * 
	 * @param path
	 * @param server
	 * @param file
	 * @return
	 * @throws LogicException
	 */
	UploadedFile uploadMetaweblogFile(MultipartFile file) throws LogicException;
}
