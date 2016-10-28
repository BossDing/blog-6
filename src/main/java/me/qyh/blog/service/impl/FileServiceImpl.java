package me.qyh.blog.service.impl;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import me.qyh.blog.bean.BlogFilePageResult;
import me.qyh.blog.bean.ExpandedCommonFile;
import me.qyh.blog.bean.UploadedFile;
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
import me.qyh.blog.file.FileServer;
import me.qyh.blog.file.FileStore;
import me.qyh.blog.pageparam.BlogFileQueryParam;
import me.qyh.blog.pageparam.PageResult;
import me.qyh.blog.service.FileService;
import me.qyh.blog.web.controller.form.BlogFileUpload;

/**
 * {@link http://mikehillyer.com/articles/managing-hierarchical-data-in-mysql}
 * 
 * @author Administrator
 *
 */
@Service
@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
public class FileServiceImpl implements FileService {

	@Autowired
	private FileManager fileManager;
	@Autowired
	private BlogFileDao blogFileDao;
	@Autowired
	private FileDeleteDao fileDeleteDao;
	@Autowired
	private CommonFileDao commonFileDao;

	private static final Logger logger = LoggerFactory.getLogger(FileServiceImpl.class);

	@Override
	public List<UploadedFile> upload(BlogFileUpload upload) throws LogicException {
		BlogFile parent;
		if (upload.getParent() != null) {
			parent = blogFileDao.selectById(upload.getParent());
			if (parent == null) {
				throw new LogicException("file.parent.notexists", "父目录不存在");
			}
		} else {
			parent = blogFileDao.selectRoot();
		}
		FileServer fs = fileManager.getFileServer(upload.getServer());
		if (fs == null) {
			throw new LogicException("file.store.notexists", "文件存储器不存在");
		}
		String folderKey = getFilePath(parent);
		synchronized (this) {
			deleteImmediatelyIfNeed(folderKey);
		}
		List<UploadedFile> uploadedFiles = new ArrayList<UploadedFile>();
		for (MultipartFile file : upload.getFiles()) {
			try {
				if (blogFileDao.selectByParentAndPath(parent, file.getOriginalFilename()) != null)
					throw new LogicException("file.path.exists", "文件已经存在");
				String key = folderKey.isEmpty() ? file.getOriginalFilename()
						: (folderKey + SPLIT_CHAR + file.getOriginalFilename());
				CommonFile cf = null;
				try {
					synchronized (fs) {
						deleteImmediatelyIfNeed(key);
						cf = fs.store(key, file);
					}
				} catch (IOException e) {
					throw new SystemException(e.getMessage(), e);
				}
				cf.setServer(fs.id());
				uploadedFiles.add(new UploadedFile(file.getOriginalFilename(), cf.getSize(),
						fs.getFileStore(cf.getStore()).getPreviewUrl(key)));
				commonFileDao.insert(cf);
				BlogFile blogFile = new BlogFile();
				blogFile.setCf(cf);
				blogFile.setPath(file.getOriginalFilename());
				blogFile.setCreateDate(Timestamp.valueOf(LocalDateTime.now()));
				blogFile.setLft(parent.getLft() + 1);
				blogFile.setRgt(parent.getLft() + 2);
				blogFile.setName(cf.getOriginalFilename());
				blogFile.setParent(parent);
				blogFile.setType(BlogFileType.FILE);

				blogFileDao.updateWhenAddChild(parent);
				blogFileDao.insert(blogFile);
			} catch (LogicException e) {
				uploadedFiles.add(new UploadedFile(file.getOriginalFilename(), e.getLogicMessage()));
			}
		}
		return uploadedFiles;
	}

	private void deleteImmediatelyIfNeed(String key) throws LogicException {
		if (key.isEmpty())
			return;
		String rootKey = key.split(SPLIT_CHAR)[0];
		List<FileDelete> children = fileDeleteDao.selectChildren(rootKey);
		if (children.isEmpty())
			return;
		for (FileDelete child : children) {
			String ckey = child.getKey();
			if (key.startsWith(ckey))
				deleteFile(child);
		}
	}

	private void deleteFile(FileDelete fd) throws LogicException {
		// 需要删除
		String key = fd.getKey();
		switch (fd.getType()) {
		case DIRECTORY:// 需要调用每个存储器，因为文件夹下的文件可能来自于不同的存储器
			for (FileServer fs : fileManager.getAllServers()) {
				for (FileStore store : fs.allStore()) {
					if (!store.deleteBatch(key))
						throw new LogicException("file.batchDelete.fail", "存储器" + store.id() + "无法删除目录" + key + "下的文件",
								store.id(), key);
				}
			}
			fileDeleteDao.deleteById(fd.getId());
			break;
		case FILE:
			FileServer server = fileManager.getFileServer(fd.getServer());
			if (server == null) {
				logger.warn("无法找到id为" + server + "的存储服务");
				fileDeleteDao.deleteById(fd.getId());
				return;
			}
			FileStore fs = server.getFileStore(fd.getStore());
			if (fs == null) {
				logger.warn("无法在存储器" + fd.getServer() + "找到id为" + fd.getStore() + "的存储器");
				fileDeleteDao.deleteById(fd.getId());
				return;
			}
			if (!fs.delete(key))
				throw new LogicException("file.delete.fail", "文件删除失败，无法删除存储器" + fs.id() + "下" + key + "对应的文件", fs.id(),
						key);
			fileDeleteDao.deleteById(fd.getId());
			break;
		}

	}

	@Override
	public void createFolder(BlogFile toCreate) throws LogicException {
		BlogFile parent = toCreate.getParent();
		if (parent != null) {
			parent = blogFileDao.selectById(parent.getId());
			if (parent == null) {
				throw new LogicException("file.parent.notexists", "父目录不存在");
			}
			if (!parent.isDir()) {
				throw new LogicException("file.parent.mustDir", "父目录必须是一个文件夹");
			}
		} else {
			// 查询根节点
			parent = blogFileDao.selectRoot();
		}

		if (blogFileDao.selectByParentAndPath(parent, toCreate.getPath()) != null)
			throw new LogicException("folder.path.exists", "文件已经存在");

		toCreate.setLft(parent.getLft() + 1);
		toCreate.setRgt(parent.getLft() + 2);
		toCreate.setParent(parent);
		toCreate.setCreateDate(Timestamp.valueOf(LocalDateTime.now()));

		blogFileDao.updateWhenAddChild(parent);
		blogFileDao.insert(toCreate);
	}

	@Override
	@Transactional(readOnly = true)
	public BlogFilePageResult queryBlogFiles(BlogFileQueryParam param) throws LogicException {
		List<BlogFile> paths = null;
		if (param.getParent() != null) {
			BlogFile parent = blogFileDao.selectById(param.getParent());
			if (parent == null) {
				throw new LogicException("file.parent.notexists", "父目录不存在");
			}
			if (!parent.isDir()) {
				throw new LogicException("file.parent.mustDir", "父目录必须是一个文件夹");
			}
			paths = blogFileDao.selectPath(parent);
		} else {
			param.setParent(blogFileDao.selectRoot().getId());
		}
		int count = blogFileDao.selectCount(param);
		List<BlogFile> datas = blogFileDao.selectPage(param);
		for (BlogFile file : datas) {
			setExpandedCommonFile(file);
		}

		PageResult<BlogFile> page = new PageResult<BlogFile>(param, count, datas);
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
			throw new LogicException("file.notexists", "文件不存在");
		}
		Map<String, Object> proMap = new HashMap<String, Object>();
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
		proMap.put("lastModifyDate", file.getLastModifyDate());
		return proMap;
	}

	@Override
	public List<FileServer> allServers() {
		return fileManager.getAllServers();
	}

	@Override
	public void update(BlogFile toUpdate) throws LogicException {
		BlogFile db = blogFileDao.selectById(toUpdate.getId());
		if (db == null) {
			throw new LogicException("file.notexists", "文件不存在");
		}
		toUpdate.setLastModifyDate(Timestamp.valueOf(LocalDateTime.now()));
		blogFileDao.update(toUpdate);
	}

	@Override
	public void delete(Integer id) throws LogicException {
		final BlogFile db = blogFileDao.selectById(id);
		if (db == null) {
			throw new LogicException("file.notexists", "文件不存在");
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

		FileServer server = null;
		FileStore store = null;
		if (db.isFile()) {
			server = fileManager.getFileServer(db.getCf().getServer());
			if (server == null) {
				return;
			}
			store = server.getFileStore(db.getCf().getStore());
			if (store == null) {
				return;
			}
		} else {
			fileDeleteDao.deleteChildren(key);
		}
		FileDelete fd = new FileDelete();
		fd.setKey(key);
		fd.setServer(db.isDir() ? null : server.id());
		fd.setStore(db.isDir() ? null : store.id());
		fd.setType(db.getType());
		fileDeleteDao.insert(fd);

	}

	@Override
	public void clearDeletedCommonFile() {
		List<FileDelete> all = fileDeleteDao.selectAll();
		for (FileDelete fd : all)
			try {
				deleteFile(fd);
			} catch (LogicException e) {
				// ignore;
			}
	}

	private void setExpandedCommonFile(BlogFile bf) {
		CommonFile cf = bf.getCf();
		if (cf != null) {
			if (bf.isFile()) {
				String key = getFilePath(bf);
				FileStore fs = getFileStore(cf);
				ExpandedCommonFile pcf = new ExpandedCommonFile();
				BeanUtils.copyProperties(cf, pcf);
				pcf.setPreviewUrl(fs.getPreviewUrl(key));
				pcf.setDownloadUrl(fs.getDownloadUrl(key));
				pcf.setUrl(fs.getUrl(key));
				bf.setCf(pcf);
			}
		}
	}

	private FileStore getFileStore(CommonFile cf) {
		FileServer server = fileManager.getFileServer(cf.getServer());
		if (server == null) {
			throw new SystemException("没有找到ID为:" + cf.getServer() + "的文件服务");
		}
		FileStore fs = server.getFileStore(cf.getStore());
		if (fs == null) {
			throw new SystemException("没有找到ID为:" + cf.getStore() + "的存储器");
		}
		return fs;
	}

	private String getFilePath(BlogFile bf) {
		List<BlogFile> files = blogFileDao.selectPath(bf);
		StringBuilder path = new StringBuilder();
		for (BlogFile file : files) {
			if (file.getPath().isEmpty())
				continue;
			path.append(file.getPath()).append(SPLIT_CHAR);
		}
		if (path.length() > 0)
			path.deleteCharAt(path.length() - 1);
		return path.toString();
	}

}
