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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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
import me.qyh.blog.file.ImageHelper;
import me.qyh.blog.message.Message;
import me.qyh.blog.pageparam.BlogFileQueryParam;
import me.qyh.blog.pageparam.PageResult;
import me.qyh.blog.service.ConfigService;
import me.qyh.blog.service.FileService;
import me.qyh.blog.util.FileUtils;
import me.qyh.blog.util.Validators;
import me.qyh.blog.web.controller.form.BlogFileUpload;
import me.qyh.blog.web.controller.form.BlogFileValidator;

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

	private static final int MAX_FILE_NAME_LENGTH = BlogFileValidator.MAX_FILE_NAME_LENGTH;

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
	public synchronized List<UploadedFile> upload(BlogFileUpload upload) throws LogicException {
		BlogFile parent;
		if (upload.getParent() != null) {
			parent = blogFileDao.selectById(upload.getParent());
			if (parent == null) {
				throw new LogicException(PARENT_NOT_EXISTS);
			}
		} else {
			parent = blogFileDao.selectRoot();
		}

		String folderKey = getFilePath(parent);
		deleteImmediatelyIfNeed(folderKey);
		Integer storeId = upload.getStore();
		if (storeId == null) {
			throw new LogicException("file.store.notexists", "文件存储器不存在");
		}
		FileStore store = fileManager.getFileStore(upload.getStore())
				.orElseThrow(() -> new LogicException("file.store.notexists", "文件存储器不存在"));

		if (store.readOnly()) {
			throw new LogicException("file.store.readonly", "只读存储器无法存储文件");
		}
		List<UploadedFile> uploadedFiles = new ArrayList<>();
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
				String extension = FileUtils.getFileExtension(originalFilename);
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
		BlogFile checked = blogFileDao.selectByParentAndPath(parent, originalFilename);
		if (checked != null) {
			throw new LogicException("file.path.exists", "文件已经存在");
		}
		String ext = FileUtils.getFileExtension(originalFilename);
		String name = FileUtils.getNameWithoutExtension(originalFilename);
		String fullname = name + "." + ext.toLowerCase();
		String key = folderKey.isEmpty() ? fullname : (folderKey + SPLIT_CHAR + fullname);
		deleteImmediatelyIfNeed(key);
		CommonFile cf = store.store(key, file);

		// 如果不是被支持的图片格式
		if (ImageHelper.isSystemAllowedImage(ext) && !ImageHelper.isSystemAllowedImage(cf.getExtension())) {
			store.delete(key);
			throw new LogicException("file.unsupportformat", "不支持" + cf.getExtension() + "格式的文件", cf.getExtension());
		}

		try {
			commonFileDao.insert(cf);
			blogFile = new BlogFile();
			blogFile.setCf(cf);
			blogFile.setPath(fullname);
			blogFile.setCreateDate(Timestamp.valueOf(LocalDateTime.now()));
			blogFile.setLft(parent.getLft() + 1);
			blogFile.setRgt(parent.getLft() + 2);
			blogFile.setParent(parent);
			blogFile.setType(BlogFileType.FILE);

			blogFileDao.updateWhenAddChild(parent);
			blogFileDao.insert(blogFile);
			return new UploadedFile(originalFilename, cf.getSize(), store.getThumbnailUrl(key).orElse(null),
					store.getUrl(key));
		} catch (RuntimeException | Error e) {
			store.delete(key);
			throw e;
		}
	}

	private void deleteImmediatelyIfNeed(String key) throws LogicException {
		if (key.isEmpty()) {
			return;
		}
		String rootKey = key.split(SPLIT_CHAR)[0];
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
		Optional<FileStore> optionalFileStore = fileManager.getFileStore(fd.getStore());
		if (!optionalFileStore.isPresent()) {
			LOGGER.warn("无法找到id为" + fd.getStore() + "的存储器");
			fileDeleteDao.deleteById(fd.getId());
			return;
		}
		FileStore fs = optionalFileStore.get();
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

		BlogFile checked = blogFileDao.selectByParentAndPath(parent, toCreate.getPath());
		if (checked != null) {
			throw new LogicException("file.path.exists", "文件已经存在");
		}

		toCreate.setLft(parent.getLft() + 1);
		toCreate.setRgt(parent.getLft() + 2);
		toCreate.setParent(parent);
		toCreate.setCreateDate(Timestamp.valueOf(LocalDateTime.now()));

		blogFileDao.updateWhenAddChild(parent);
		blogFileDao.insert(toCreate);
	}

	private BlogFile createFolder(String path) throws LogicException {
		String cleanedPath = FileService.cleanPath(path);
		if (cleanedPath.isEmpty()) {
			return blogFileDao.selectRoot();
		} else {
			if (cleanedPath.indexOf(FileService.SPLIT_CHAR) == -1) {
				return createFolder(blogFileDao.selectRoot(), cleanedPath);
			} else {
				BlogFile parent = blogFileDao.selectRoot();
				for (String _path : cleanedPath.split(SPLIT_CHAR)) {
					parent = createFolder(parent, _path);
				}
				return parent;
			}
		}
	}

	private BlogFile createFolder(BlogFile parent, String folder) throws LogicException {
		BlogFile checked = blogFileDao.selectByParentAndPath(parent, folder);
		if (checked != null) {
			if (checked.isDir()) {
				return checked;
			} else {
				throw new LogicException("file.path.exists", "文件已经存在");
			}
		}
		BlogFile bf = new BlogFile();
		bf.setCreateDate(Timestamp.valueOf(LocalDateTime.now()));
		bf.setLft(parent.getLft() + 1);
		bf.setRgt(parent.getLft() + 2);
		bf.setParent(parent);
		bf.setPath(folder);
		bf.setType(BlogFileType.DIRECTORY);
		blogFileDao.updateWhenAddChild(parent);
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
			param.setParentFile(parent);
		} else {
			param.setParentFile(blogFileDao.selectRoot());
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
		Map<String, Object> proMap = new HashMap<>();
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
	public List<FileStore> allStorableStores() {
		return fileManager.getAllStores().stream().filter(store -> !store.readOnly()).collect(Collectors.toList());
	}

	@Override
	public synchronized void update(BlogFile toUpdate) throws LogicException {
		BlogFile db = blogFileDao.selectById(toUpdate.getId());
		if (db == null) {
			throw new LogicException(NOT_EXISTS);
		}
		if (db.getParent() == null) {
			throw new LogicException("file.root.canNotUpdate", "根节点不能更新");
		}
		if (db.isDir()) {
			db.setPath(null);
			blogFileDao.update(db);
		} else {
			String oldPath = Validators.cleanPath(getFilePath(db));
			String path = Validators.cleanPath(toUpdate.getPath());
			// 需要更新路径
			// 这里只允许文件更新，文件夹更新无法保证文件夹内文件全部更新
			// oss存储没有文件夹的概念
			String ext = FileUtils.getFileExtension(oldPath);
			// 不能更改后缀
			if (!Validators.isEmptyOrNull(ext, true)) {
				path = path + "." + ext;
			}
			if (!oldPath.equals(path)) {
				int index = path.lastIndexOf(FileService.SPLIT_CHAR);
				String folderPath = index == -1 ? "" : path.substring(0, index);
				String fileName = index == -1 ? path : path.substring(index + 1, path.length());

				if (fileName.length() > MAX_FILE_NAME_LENGTH) {
					int extLength = ext.length() + 1;
					if (extLength >= MAX_FILE_NAME_LENGTH) {
						throw new LogicException("file.ext.toolong", "该文件后缀名过长，无法更新路径");
					} else {
						throw new LogicException("file.name.toolong",
								"文件名不能超过" + (MAX_FILE_NAME_LENGTH - extLength) + "个字符", fileName,
								(MAX_FILE_NAME_LENGTH - extLength));
					}
				}

				// 先删除节点
				blogFileDao.delete(db);
				blogFileDao.updateWhenDelete(db);

				BlogFile parent = createFolder(folderPath);
				BlogFile checked = blogFileDao.selectByParentAndPath(parent, fileName);
				// 路径上存在文件
				if (checked != null) {
					throw new LogicException("file.path.exists", "文件已经存在");
				}
				FileStore fs;

				fs = getFileStore(db.getCf());
				if (!fs.copy(oldPath, path)) {
					throw new LogicException("file.move.fail", "文件移动失败");
				}

				// 再插入节点
				blogFileDao.updateWhenAddChild(parent);

				BlogFile bf = new BlogFile();
				bf.setCf(db.getCf());
				bf.setCreateDate(db.getCreateDate());
				bf.setLft(parent.getLft() + 1);
				bf.setRgt(parent.getLft() + 2);
				bf.setParent(parent);
				bf.setPath(fileName);
				bf.setType(BlogFileType.FILE);

				blogFileDao.insert(bf);

				if (!fs.delete(oldPath)) {
					FileDelete fd = new FileDelete();
					fd.setKey(oldPath);
					fd.setStore(fs.id());
					fd.setType(BlogFileType.FILE);
					fileDeleteDao.insert(fd);
				}
			} else {
				db.setPath(null);
				blogFileDao.update(db);
			}
		}
	}

	@Override
	public void delete(Integer id) throws LogicException {
		BlogFile db = blogFileDao.selectById(id);
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
		blogFileDao.updateWhenDelete(db);

		FileDelete fd = new FileDelete();
		if (db.isFile()) {
			fileManager.getFileStore(db.getCf().getStore()).ifPresent(store -> fd.setStore(store.id()));
		} else {
			fileDeleteDao.deleteChildren(key);
		}
		fd.setKey(key);
		fd.setType(db.getType());
		fileDeleteDao.insert(fd);

	}

	@Override
	public synchronized void clearDeletedCommonFile() {
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

	/**
	 * {@inheritDoc}
	 * 
	 * path层次过深会影响效率
	 */
	@Override
	@Transactional(readOnly = true)
	public PageResult<BlogFile> queryFiles(String path, Set<String> extensions, int page) {
		BlogFileQueryParam param = new BlogFileQueryParam();
		param.setCurrentPage(page);
		param.setPageSize(configService.getGlobalConfig().getFilePageSize());
		BlogFile parent = blogFileDao.selectRoot();
		if (Validators.isEmptyOrNull(path, true) || FileService.SPLIT_CHAR.equals(path)) {
			param.setParentFile(parent);
		} else {
			String cleanedPath = Validators.cleanPath(path.trim());
			if (cleanedPath.indexOf(FileService.SPLIT_CHAR) == -1) {
				parent = blogFileDao.selectByParentAndPath(parent, cleanedPath);
			} else {
				for (String _path : cleanedPath.split(SPLIT_CHAR)) {
					parent = blogFileDao.selectByParentAndPath(parent, _path);
					if (parent == null) {
						break;
					}
				}
			}

			if (parent == null || parent.isFile()) {
				return new PageResult<>(param, 0, Collections.emptyList());
			}

			param.setParentFile(parent);
		}
		param.setType(BlogFileType.FILE);
		param.setQuerySubDir(true);
		param.setExtensions(extensions);

		int count = blogFileDao.selectCount(param);
		List<BlogFile> datas = blogFileDao.selectPage(param);

		for (BlogFile file : datas) {
			setExpandedCommonFile(file);
		}

		return new PageResult<>(param, count, datas);
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<BlogFile> getFile(int id) {
		BlogFile file = blogFileDao.selectById(id);
		if (file.getParent() == null) {
			file = null;
		}
		if (file != null) {
			file.setPath(getFilePath(file));
		}
		return Optional.ofNullable(file);
	}

	private void setExpandedCommonFile(BlogFile bf) {
		CommonFile cf = bf.getCf();
		if (cf != null && bf.isFile()) {
			String key = getFilePath(bf);
			FileStore fs = getFileStore(cf);
			ExpandedCommonFile pcf = new ExpandedCommonFile();
			BeanUtils.copyProperties(cf, pcf);
			pcf.setThumbnailUrl(fs.getThumbnailUrl(key).orElse(null));
			pcf.setDownloadUrl(fs.getDownloadUrl(key));
			pcf.setUrl(fs.getUrl(key));
			bf.setCf(pcf);
		}
	}

	private FileStore getFileStore(CommonFile cf) {
		return fileManager.getFileStore(cf.getStore())
				.orElseThrow(() -> new SystemException("没有找到ID为:" + cf.getStore() + "的存储器"));
	}

	private String getFilePath(BlogFile bf) {
		List<BlogFile> files = blogFileDao.selectPath(bf);
		return files.stream().map(BlogFile::getPath).filter(path -> !path.isEmpty())
				.collect(Collectors.joining(SPLIT_CHAR));
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		// 找根目录
		if (blogFileDao.selectRoot() == null) {
			LOGGER.debug("没有找到任何根目录，将创建一个根目录");
			BlogFile root = new BlogFile();
			root.setCreateDate(Timestamp.valueOf(LocalDateTime.now()));
			root.setLft(1);
			root.setPath("");
			root.setRgt(2);
			root.setType(BlogFileType.DIRECTORY);
			blogFileDao.insert(root);
		}
	}
}
