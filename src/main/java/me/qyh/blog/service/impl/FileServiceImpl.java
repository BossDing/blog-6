package me.qyh.blog.service.impl;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import me.qyh.blog.bean.BlogFilePageResult;
import me.qyh.blog.bean.BlogFileProperty;
import me.qyh.blog.bean.ExpandedCommonFile;
import me.qyh.blog.bean.UploadedFile;
import me.qyh.blog.dao.BlogFileDao;
import me.qyh.blog.dao.CommonFileDao;
import me.qyh.blog.entity.BlogFile;
import me.qyh.blog.entity.BlogFile.BlogFileType;
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.exception.SystemException;
import me.qyh.blog.file.CommonFile;
import me.qyh.blog.file.CommonFile.CommonFileStatus;
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
	private CommonFileDao commonFileDao;

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
		List<UploadedFile> uploadedFiles = new ArrayList<UploadedFile>();
		for (MultipartFile file : upload.getFiles()) {
			try {
				CommonFile cf = null;
				try {
					cf = fs.store(file);
				} catch (IOException e) {
					throw new SystemException(e.getMessage(), e);
				}
				cf.setServer(fs.id());
				uploadedFiles.add(new UploadedFile(file.getOriginalFilename(), cf.getSize(),
						fs.getFileStore(cf.getStore()).getPreviewUrl(cf)));
				cf.setStatus(CommonFileStatus.NORMAL);
				commonFileDao.insert(cf);
				BlogFile blogFile = new BlogFile();
				blogFile.setCf(cf);
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
	public BlogFileProperty getBlogFileProperty(Integer id) throws LogicException {
		BlogFile file = blogFileDao.selectById(id);
		if (file == null) {
			throw new LogicException("file.notexists", "文件不存在");
		}
		BlogFileProperty property = new BlogFileProperty();
		if (file.isDir()) {
			property.setCounts(blogFileDao.selectSubBlogFileCount(file));
			property.setTotalSize(blogFileDao.selectSubBlogFileSize(file));
		} else {
			CommonFile cf = file.getCf();
			if (cf != null) {
				FileStore fs = getFileStore(cf);
				property.setUrl(fs.getUrl(cf));
				property.setDownloadUrl(fs.getDownloadUrl(cf));
				property.setTotalSize(cf.getSize());
			}
		}
		property.setType(file.getType());
		property.setLastModifyDate(file.getLastModifyDate());
		return property;
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
		BlogFile db = blogFileDao.selectById(id);
		if (db == null) {
			throw new LogicException("file.notexists", "文件不存在");
		}
		if (db.getParent() == null) {
			throw new LogicException("file.root.canNotDelete", "根节点不能删除");
		}
		// 首先将关联的CommonFile置为待删除状态
		// 因为CommonFile跟实际文件关联
		blogFileDao.deleteCommonFile(db);
		// 删除节点以及子节点
		blogFileDao.delete(db);
		// 更新受影响节点的左右值
		blogFileDao.updateWhenDelete(db);
	}

	@Override
	public void clearDeletedCommonFile() {
		List<CommonFile> toDeletes = commonFileDao.selectDeleted();
		if (!toDeletes.isEmpty()) {
			for (CommonFile cf : toDeletes) {
				FileStore fs = getFileStore(cf);
				if (fs.delete(cf)) {
					commonFileDao.deleteById(cf.getId());
				}
			}
		}
	}

	/**
	 * 移动节点
	 * {@link http://stackoverflow.com/questions/889527/move-node-in-nested-set}
	 * 
	 * @param srcId
	 * @param parentId
	 * @throws LogicException
	 */
	@Override
	public void move(Integer srcId, Integer parentId) throws LogicException {
		if (srcId != null && srcId.equals(parentId)) {
			throw new LogicException("file.src.noEqualDest", "源节点和目标节点不能相同");
		}
		BlogFile src = blogFileDao.selectById(srcId);
		if (src == null) {
			throw new LogicException("file.src.notexists", "源文件节点不存在");
		}
		if (src.isRoot()) {
			throw new LogicException("file.root.noMove", "根节点不能被移动");
		}
		BlogFile parent = null;
		if (parentId != null) {
			parent = blogFileDao.selectById(parentId);
			if (parent == null) {
				throw new LogicException("file.dest.notexists", "目标文件节点不存在");
			}
			if (!parent.isDir()) {
				throw new LogicException("file.dest.mustDir", "目标文件节点必须是一个文件夹");
			}
			// 查看目标节点是不是当前结点的子节点
			List<BlogFile> paths = blogFileDao.selectPath(parent);
			if (!paths.isEmpty() && paths.contains(src)) {
				throw new LogicException("file.dest.notSrcChild", "目标文件节点不能是待移动节点的子节点");
			}
		} else {
			parent = blogFileDao.selectRoot();
		}
		// 当前节点的父节点就是目标节点
		if (src.getParent().equals(parent)) {
			return;
		}
		blogFileDao.updateWhenMove(src, parent);
	}

	private void setExpandedCommonFile(BlogFile bf) {
		CommonFile cf = bf.getCf();
		if (cf != null) {
			bf.setCf(convert(cf));
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

	private ExpandedCommonFile convert(CommonFile cf) {
		FileStore fs = getFileStore(cf);
		ExpandedCommonFile pcf = new ExpandedCommonFile();
		BeanUtils.copyProperties(cf, pcf);
		pcf.setPreviewUrl(fs.getPreviewUrl(cf));
		pcf.setDownloadUrl(fs.getDownloadUrl(cf));
		pcf.setUrl(fs.getUrl(cf));
		return pcf;
	}

}
