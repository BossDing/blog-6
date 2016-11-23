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
package me.qyh.blog.file.oss;

import java.io.File;
import java.io.IOException;

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
import me.qyh.blog.file.FileHelper;
import me.qyh.blog.file.FileStore;
import me.qyh.blog.file.ImageHelper;
import me.qyh.blog.file.ImageHelper.ImageInfo;
import me.qyh.blog.message.Message;

/**
 * oss文件存储抽象类
 * 
 * @author Administrator
 *
 */
public abstract class AbstractOssFileStore implements FileStore, InitializingBean {

	private int id;
	private String backupAbsPath;
	private File backupDir;

	protected static final Logger logger = LoggerFactory.getLogger(AbstractOssFileStore.class);

	@Autowired
	protected ImageHelper imageHelper;

	@Override
	public CommonFile store(String key, MultipartFile multipartFile) throws LogicException {
		String originalFilename = multipartFile.getOriginalFilename();
		String extension = FilenameUtils.getExtension(originalFilename);
		File tmp = FileHelper.temp(extension);
		try {
			multipartFile.transferTo(tmp);
		} catch (IOException e) {
			throw new SystemException(e.getMessage(), e);
		}
		try {
			ImageInfo ii = readImage(tmp, extension);
			doUpload(key, tmp);
			CommonFile cf = new CommonFile();
			cf.setExtension(extension);
			cf.setOriginalFilename(originalFilename);
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

	private ImageInfo readImage(File tmp, String extension) throws LogicException {
		if (imageHelper.supportFormat(extension)) {
			try {
				return imageHelper.read(tmp);
			} catch (IOException e) {
				logger.debug(e.getMessage(), e);
				throw new LogicException(new Message("image.corrupt", "不是正确的图片文件或者图片已经损坏"));
			}
		}
		return null;
	}

	private void doUpload(String key, File tmp) {
		File backup = null;
		try {
			if (backupDir != null) {
				backup = new File(backupDir, key);
				FileUtils.forceMkdir(backup.getParentFile());
				FileUtils.copyFile(tmp, backup);
			}
			upload(key, tmp);
		} catch (IOException e) {
			if (backup != null && !FileUtils.deleteQuietly(backup) && backup.exists()) {
				backup.deleteOnExit();
			}
			throw new SystemException(e.getMessage(), e);
		}
	}

	protected abstract void upload(String key, File file) throws IOException;

	protected boolean image(String key) {
		return imageHelper.supportFormat(FilenameUtils.getExtension(key));
	}

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
		boolean flag = doDeleteBatch(key);
		if (flag)
			flag = deleteBackup(key);
		return flag;
	}

	@Override
	public final boolean delete(String key) {
		boolean flag = doDelete(key);
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

	protected abstract boolean doDelete(String key);

	protected abstract boolean doDeleteBatch(String key);

	public void setId(int id) {
		this.id = id;
	}

	public void setBackupAbsPath(String backupAbsPath) {
		this.backupAbsPath = backupAbsPath;
	}
}
