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
package me.qyh.blog.service.impl;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Files;

import me.qyh.blog.bean.BlogFilePageResult;
import me.qyh.blog.bean.ExpandedCommonFile;
import me.qyh.blog.bean.UploadedFile;
import me.qyh.blog.config.UploadConfig;
import me.qyh.blog.dao.BlogFileDao;
import me.qyh.blog.dao.CommonFileDao;
import me.qyh.blog.dao.FileDeleteDao;
import me.qyh.blog.entity.BlogFile;
import me.qyh.blog.entity.BlogFile.BlogFileType;
import me.qyh.blog.entity.FileDelete;
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.exception.SystemException;
import me.qyh.blog.file.CommonFile;
import me.qyh.blog.file.FileManager;
import me.qyh.blog.file.FileStore;
import me.qyh.blog.message.Message;
import me.qyh.blog.pageparam.BlogFileQueryParam;
import me.qyh.blog.pageparam.PageResult;
import me.qyh.blog.service.ConfigService;
import me.qyh.blog.service.FileService;
import me.qyh.blog.util.Validators;
import me.qyh.blog.web.controller.form.BlogFileUpload;

/**
 * {@link http://mikehillyer.com/articles/managing-hierarchical-data-in-mysql}
 * 
 * @author Administrator
 *
 */
@Service
@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
public class FileServiceImpl implements FileService, InitializingBean {

	@Autowired
	private FileManager fileManager;
	@Autowired
	private BlogFileDao blogFileDao;
	@Autowired
	private FileDeleteDao fileDeleteDao;
	@Autowired
	private CommonFileDao commonFileDao;
	@Autowired
	private ConfigService configService;

	private BlogFile root;

	private static final Logger LOGGER = LoggerFactory.getLogger(FileServiceImpl.class);

	private static final Message PARENT_NOT_EXISTS = new Message("file.parent.notexists", "父目录不存在");
	private static final Message NOT_EXISTS = new Message("file.notexists", "文件不存在");

	@Override
	public synchronized UploadedFile uploadMetaweblogFile(MultipartFile file) throws LogicException {
		return upload(configService.getMetaweblogConfig(), file);
	}

	private UploadedFile upload(UploadConfig config, MultipartFile file) throws LogicException {
		BlogFile parent = createFolder(config.getPath());
		BlogFileUpload bfu = new BlogFileUpload();
		bfu.setFiles(Arrays.asList(file));
		bfu.setParent(parent.getId());
		bfu.setStore(config.getStore());
		return upload(bfu).get(0);
	}

	@Override
	public List<UploadedFile> upload(BlogFileUpload upload) throws LogicException {
		BlogFile parent;
		if (upload.getParent() != null) {
			parent = blogFileDao.selectById(upload.getParent());
			if (parent == null) {
				throw new LogicException(PARENT_NOT_EXISTS);
			}
		} else {
			parent = root;
		}

		String folderKey = getFilePath(parent);
		synchronized (this) {
			deleteImmediatelyIfNeed(folderKey);
		}
		Integer storeId = upload.getStore();
		if (storeId == null) {
			throw new LogicException("file.store.notexists", "文件存储器不存在");
		}
		FileStore store = fileManager.getFileStore(upload.getStore());
		if (store == null) {
			throw new LogicException("file.store.notexists", "文件存储器不存在");
		}
		List<UploadedFile> uploadedFiles = Lists.newArrayList();
		for (MultipartFile file : upload.getFiles()) {
			String originalFilename = file.getOriginalFilename();
			if (store.canStore(file)) {
				try {
					UploadedFile uf = storeMultipartFile(file, parent, folderKey, store);
					uploadedFiles.add(uf);
				} catch (LogicException e) {
					uploadedFiles.add(new UploadedFile(originalFilename, e.getLogicMessage()));
				}
			} else {
				String extension = Files.getFileExtension(originalFilename);
				uploadedFiles.add(new UploadedFile(originalFilename,
						new Message("file.store.unsupportformat", "存储器不支持存储" + extension + "文件", extension)));
			}
		}
		return uploadedFiles;
	}

	private UploadedFile storeMultipartFile(MultipartFile file, BlogFile parent, String folderKey, FileStore store)
			throws LogicException {
		BlogFile blogFile;
		String originalFilename = file.getOriginalFilename();
		if (blogFileDao.selectByParentAndPath(parent, originalFilename) != null) {
			throw new LogicException("file.path.exists", "文件已经存在");
		}
		String key = folderKey.isEmpty() ? originalFilename : (folderKey + SPLIT_CHAR + originalFilename);
		CommonFile cf = null;
		synchronized (this) {
			deleteImmediatelyIfNeed(key);
			cf = store.store(key, file);
		}
		try {
			commonFileDao.insert(cf);
			blogFile = new BlogFile();
			blogFile.setCf(cf);
			blogFile.setPath(originalFilename);
			blogFile.setCreateDate(Timestamp.valueOf(LocalDateTime.now()));
			blogFile.setLft(parent.getLft() + 1);
			blogFile.setRgt(parent.getLft() + 2);
			blogFile.setName(originalFilename);
			blogFile.setParent(parent);
			blogFile.setType(BlogFileType.FILE);

			blogFileDao.updateLftWhenAddChild(parent);
			blogFileDao.updateRgtWhenAddChild(parent);
			blogFileDao.insert(blogFile);
			return new UploadedFile(originalFilename, cf.getSize(), store.getThumbnailUrl(key), store.getUrl(key));
		} catch (RuntimeException | Error e) {
			// delete file;
			store.delete(key);
			throw e;
		}
	}

	private void deleteImmediatelyIfNeed(String key) throws LogicException {
		if (key.isEmpty()) {
			return;
		}
		String rootKey = Splitter.on(SPLIT_CHAR).split(key).iterator().next();
		List<FileDelete> children = fileDeleteDao.selectChildren(rootKey);
		if (children.isEmpty()) {
			return;
		}
		for (FileDelete child : children) {
			String ckey = child.getKey();
			if (key.startsWith(ckey)) {
				deleteFile(child);
			}
		}
	}

	private void deleteFile(FileDelete fd) throws LogicException {
		if (fd.getType().equals(BlogFileType.DIRECTORY)) {
			deleteDirectory(fd);
		} else {
			deleteOne(fd);
		}
	}

	private void deleteDirectory(FileDelete fd) throws LogicException {
		String key = fd.getKey();
		for (FileStore store : fileManager.getAllStores()) {
			if (!store.deleteBatch(key)) {
				throw new LogicException("file.batchDelete.fail", "存储器" + store.id() + "无法删除目录" + key + "下的文件",
						store.id(), key);
			}
		}
		fileDeleteDao.deleteById(fd.getId());
	}

	private void deleteOne(FileDelete fd) throws LogicException {
		String key = fd.getKey();
		FileStore fs = fileManager.getFileStore(fd.getStore());
		if (fs == null) {
			LOGGER.warn("无法找到id为" + fd.getStore() + "的存储器");
			fileDeleteDao.deleteById(fd.getId());
			return;
		}
		if (!fs.delete(key)) {
			throw new LogicException("file.delete.fail", "文件删除失败，无法删除存储器" + fs.id() + "下" + key + "对应的文件", fs.id(),
					key);
		}
		fileDeleteDao.deleteById(fd.getId());
	}

	@Override
	public void createFolder(BlogFile toCreate) throws LogicException {
		BlogFile parent = toCreate.getParent();
		if (parent != null) {
			parent = blogFileDao.selectById(parent.getId());
			if (parent == null) {
				throw new LogicException(PARENT_NOT_EXISTS);
			}
			if (!parent.isDir()) {
				throw new LogicException("file.parent.mustDir", "父目录必须是一个文件夹");
			}
		} else {
			// 查询根节点
			parent = blogFileDao.selectRoot();
		}

		if (blogFileDao.selectByParentAndPath(parent, toCreate.getPath()) != null) {
			throw new LogicException("folder.path.exists", "文件已经存在");
		}

		toCreate.setLft(parent.getLft() + 1);
		toCreate.setRgt(parent.getLft() + 2);
		toCreate.setParent(parent);
		toCreate.setCreateDate(Timestamp.valueOf(LocalDateTime.now()));

		blogFileDao.updateLftWhenAddChild(parent);
		blogFileDao.updateRgtWhenAddChild(parent);
		blogFileDao.insert(toCreate);
	}

	private BlogFile createFolder(String path) {
		if (Validators.isEmptyOrNull(path, true)) {
			return root;
		} else {
			if (path.indexOf(FileService.SPLIT_CHAR) == -1) {
				return createFolder(root, path);
			} else {
				BlogFile parent = root;
				for (String _path : Splitter.on(SPLIT_CHAR).split(path)) {
					parent = createFolder(parent, _path);
				}
				return parent;
			}
		}
	}

	private BlogFile createFolder(BlogFile parent, String folder) {
		BlogFile check = blogFileDao.selectByParentAndPath(parent, folder);
		if (check != null) {
			return check;
		}
		BlogFile bf = new BlogFile();
		bf.setCreateDate(Timestamp.valueOf(LocalDateTime.now()));
		bf.setLft(parent.getLft() + 1);
		bf.setRgt(parent.getLft() + 2);
		bf.setParent(parent);
		bf.setName(folder);
		bf.setPath(folder);
		bf.setType(BlogFileType.DIRECTORY);
		blogFileDao.updateLftWhenAddChild(parent);
		blogFileDao.updateRgtWhenAddChild(parent);
		blogFileDao.insert(bf);
		return bf;
	}

	@Override
	@Transactional(readOnly = true)
	public BlogFilePageResult queryBlogFiles(BlogFileQueryParam param) throws LogicException {
		List<BlogFile> paths = null;
		if (param.getParent() != null) {
			BlogFile parent = blogFileDao.selectById(param.getParent());
			if (parent == null) {
				throw new LogicException(PARENT_NOT_EXISTS);
			}
			if (!parent.isDir()) {
				throw new LogicException("file.parent.mustDir", "父目录必须是一个文件夹");
			}
			paths = blogFileDao.selectPath(parent);
		} else {
			param.setParent(root.getId());
		}
		param.setPageSize(configService.getGlobalConfig().getFilePageSize());
		int count = blogFileDao.selectCount(param);
		List<BlogFile> datas = blogFileDao.selectPage(param);
		for (BlogFile file : datas) {
			setExpandedCommonFile(file);
		}

		PageResult<BlogFile> page = new PageResult<>(param, count, datas);
		BlogFilePageResult result = new BlogFilePageResult();
		result.setPage(page);
		if (paths != null) {
			paths.remove(0);
			result.setPaths(paths);
		}
		return result;
	}

	@Override
	@Transactional(readOnly = true)
	public Map<String, Object> getBlogFileProperty(Integer id) throws LogicException {
		BlogFile file = blogFileDao.selectById(id);
		if (file == null) {
			throw new LogicException(NOT_EXISTS);
		}
		Map<String, Object> proMap = Maps.newHashMap();
		if (file.isDir()) {
			proMap.put("counts", blogFileDao.selectSubBlogFileCount(file));
			proMap.put("totalSize", blogFileDao.selectSubBlogFileSize(file));
		} else {
			CommonFile cf = file.getCf();
			if (cf != null) {
				String key = getFilePath(file);
				FileStore fs = getFileStore(cf);
				proMap.put("url", fs.getUrl(key));
				proMap.put("downloadUrl", fs.getDownloadUrl(key));
				proMap.put("thumbUrl", fs.getThumbnailUrl(key));
				proMap.put("totalSize", cf.getSize());
				if (cf.getWidth() != null) {
					proMap.put("width", cf.getWidth());
				}
				if (cf.getHeight() != null) {
					proMap.put("height", cf.getHeight());
				}
			}
		}
		proMap.put("type", file.getType());
		return proMap;
	}

	@Override
	public List<FileStore> allStores() {
		return fileManager.getAllStores();
	}

	@Override
	public void update(BlogFile toUpdate) throws LogicException {
		BlogFile db = blogFileDao.selectById(toUpdate.getId());
		if (db == null) {
			throw new LogicException(NOT_EXISTS);
		}
		toUpdate.setLastModifyDate(Timestamp.valueOf(LocalDateTime.now()));
		blogFileDao.update(toUpdate);
	}

	@Override
	public void delete(Integer id) throws LogicException {
		final BlogFile db = blogFileDao.selectById(id);
		if (db == null) {
			throw new LogicException(NOT_EXISTS);
		}
		if (db.getParent() == null) {
			throw new LogicException("file.root.canNotDelete", "根节点不能删除");
		}
		String key = getFilePath(db);
		// 删除文件记录
		blogFileDao.delete(db);
		blogFileDao.deleteCommonFile(db);
		// 更新受影响节点的左右值
		blogFileDao.updateLftWhenDelete(db);
		blogFileDao.updateRgtWhenDelete(db);

		FileDelete fd = new FileDelete();
		if (db.isFile()) {
			FileStore store = fileManager.getFileStore(db.getCf().getStore());
			if (store == null) {
				return;
			}
			fd.setStore(store.id());
		} else {
			fileDeleteDao.deleteChildren(key);
		}
		fd.setKey(key);
		fd.setType(db.getType());
		fileDeleteDao.insert(fd);

	}

	@Override
	public void clearDeletedCommonFile() {
		List<FileDelete> all = fileDeleteDao.selectAll();
		for (FileDelete fd : all) {
			try {
				deleteFile(fd);
			} catch (LogicException e) {
				continue;
			}
		}

		blogFileDao.deleteUnassociateCommonFile();
	}

	private void setExpandedCommonFile(BlogFile bf) {
		CommonFile cf = bf.getCf();
		if (cf != null && bf.isFile()) {
			String key = getFilePath(bf);
			FileStore fs = getFileStore(cf);
			ExpandedCommonFile pcf = new ExpandedCommonFile();
			BeanUtils.copyProperties(cf, pcf);
			pcf.setThumbnailUrl(fs.getThumbnailUrl(key));
			pcf.setDownloadUrl(fs.getDownloadUrl(key));
			pcf.setUrl(fs.getUrl(key));
			bf.setCf(pcf);
		}
	}

	private FileStore getFileStore(CommonFile cf) {
		FileStore fs = fileManager.getFileStore(cf.getStore());
		if (fs == null) {
			throw new SystemException("没有找到ID为:" + cf.getStore() + "的存储器");
		}
		return fs;
	}

	private String getFilePath(BlogFile bf) {
		List<BlogFile> files = blogFileDao.selectPath(bf);
		StringBuilder path = new StringBuilder();
		for (BlogFile file : files) {
			if (file.getPath().isEmpty()) {
				continue;
			}
			path.append(file.getPath()).append(SPLIT_CHAR);
		}
		if (path.length() > 0) {
			path.deleteCharAt(path.length() - 1);
		}
		return path.toString();
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		// 找根目录
		root = blogFileDao.selectRoot();
		if (root == null) {
			LOGGER.debug("没有找到任何根目录，将创建一个根目录");
			root = new BlogFile();
			root.setCreateDate(Timestamp.valueOf(LocalDateTime.now()));
			root.setLft(1);
			root.setName("");
			root.setPath("");
			root.setRgt(2);
			root.setType(BlogFileType.DIRECTORY);
			blogFileDao.insert(root);
		}
	}

}
