package me.qyh.blog.file.oss;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.multipart.MultipartFile;

import me.qyh.blog.exception.LogicException;
import me.qyh.blog.exception.SystemException;
import me.qyh.blog.file.CommonFile;
import me.qyh.blog.file.FileStore;
import me.qyh.blog.file.ImageHelper;
import me.qyh.blog.file.ImageHelper.ImageInfo;
import me.qyh.blog.file.local.ImageReadException;
import me.qyh.blog.message.Message;

public abstract class AbstractOssFileStore implements FileStore, InitializingBean {

	private int id;
	private String backupAbsPath;
	private File backupDir;

	protected static final Logger logger = LoggerFactory.getLogger(AbstractOssFileStore.class);

	@Autowired
	protected ImageHelper imageHelper;

	@Override
	public CommonFile store(String key, MultipartFile multipartFile) throws LogicException, IOException {
		String extension = FilenameUtils.getExtension(multipartFile.getOriginalFilename());
		File tmp = Files.createTempFile(null, "." + extension).toFile();
		ImageInfo ii = null;
		try {
			multipartFile.transferTo(tmp);
			if (ImageHelper.isImage(extension)) {
				try {
					ii = imageHelper.read(tmp);
				} catch (ImageReadException e) {
					throw new LogicException(new Message("image.corrupt", "不是正确的图片文件或者图片已经损坏"));
				}
			}
			File backup = null;
			try {
				if (backupDir != null) {
					backup = new File(backupDir, key);
					FileUtils.forceMkdir(backup.getParentFile());
					FileUtils.copyFile(tmp, backup);
				}
				upload(key, tmp);
			} catch (UploadException e) {
				if (backup != null && !FileUtils.deleteQuietly(backup) && backup.exists()) {
					backup.deleteOnExit();
				}
				throw new SystemException(e.getMessage(), e);
			}
			CommonFile cf = new CommonFile();
			cf.setExtension(extension);
			cf.setOriginalFilename(multipartFile.getOriginalFilename());
			cf.setSize(tmp.length());
			cf.setStore(id);
			if (ii != null) {
				cf.setWidth(ii.getWidth());
				cf.setHeight(ii.getHeight());
			}
			return cf;
		} finally {
			if (!FileUtils.deleteQuietly(tmp) && tmp.exists()) {
				tmp.deleteOnExit();
			}
		}
	}

	protected abstract void upload(String key, File file) throws UploadException;

	@Override
	public int id() {
		return id;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (backupAbsPath != null) {
			backupDir = new File(backupAbsPath);
			FileUtils.forceMkdir(backupDir);
		}
	}

	@Override
	public final boolean deleteBatch(String key) {
		boolean flag = _deleteBatch(key);
		if (flag)
			flag = deleteBackup(key);
		return flag;
	}

	@Override
	public final boolean delete(String key) {
		boolean flag = _delete(key);
		if (flag)
			flag = deleteBackup(key);
		return flag;
	}

	private boolean deleteBackup(String key) {
		if (backupDir != null) {
			File backup = new File(backupDir, key);
			if (backup.exists())
				return FileUtils.deleteQuietly(backup);
		}
		return false;
	}

	protected abstract boolean _delete(String key);

	protected abstract boolean _deleteBatch(String key);

	public void setId(int id) {
		this.id = id;
	}

	public void setBackupAbsPath(String backupAbsPath) {
		this.backupAbsPath = backupAbsPath;
	}

	protected class UploadException extends Exception {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public UploadException(String message, Throwable cause) {
			super(message, cause);
		}

		public UploadException(String message) {
			super(message);
		}

	}
}
