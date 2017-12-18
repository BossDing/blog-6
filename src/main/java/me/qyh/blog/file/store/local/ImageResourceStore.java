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
package me.qyh.blog.file.store.local;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import me.qyh.blog.core.exception.LogicException;
import me.qyh.blog.core.exception.SystemException;
import me.qyh.blog.core.util.FileUtils;
import me.qyh.blog.file.entity.CommonFile;
import me.qyh.blog.file.store.ImageHelper;
import me.qyh.blog.file.store.ImageHelper.ImageInfo;
import me.qyh.blog.file.store.Resize;

/**
 * 本地图片存储，图片访问
 * 
 * @author Administrator
 *
 */
public class ImageResourceStore extends ThumbnailSupport {

	/**
	 * 原图保护
	 */
	private boolean sourceProtected;

	public ImageResourceStore(String urlPatternPrefix, int semaphoreNum) {
		super(urlPatternPrefix, semaphoreNum);
	}

	public ImageResourceStore(String urlPatternPrefix) {
		this(urlPatternPrefix, 5);
	}

	public ImageResourceStore() {
		this("image");
	}

	@Override
	public CommonFile doStore(Path dest, String key, MultipartFile mf) throws LogicException {
		// 先写入临时文件
		String originalFilename = mf.getOriginalFilename();
		Path tmp = FileUtils.appTemp(FileUtils.getFileExtension(originalFilename));
		try (InputStream is = mf.getInputStream()) {
			Files.copy(is, tmp, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e1) {
			throw new SystemException(e1.getMessage(), e1);
		}
		Path finalFile = tmp;
		try {
			ImageInfo ii = readImage(tmp);
			String extension = ii.getExtension();
			FileUtils.forceMkdir(dest.getParent());
			FileUtils.move(finalFile, dest);
			CommonFile cf = new CommonFile();
			cf.setExtension(extension);
			cf.setSize(mf.getSize());
			cf.setStore(id);
			cf.setOriginalFilename(originalFilename);

			cf.setWidth(ii.getWidth());
			cf.setHeight(ii.getHeight());

			return cf;
		} catch (IOException e) {
			throw new SystemException(e.getMessage(), e);
		} finally {
			FileUtils.deleteQuietly(finalFile);
		}
	}

	private ImageInfo readImage(Path tmp) throws LogicException {
		try {
			return imageHelper.read(tmp);
		} catch (IOException e) {
			logger.debug(e.getMessage(), e);
			throw new LogicException("image.corrupt", "不是正确的图片文件或者图片已经损坏");
		}
	}

	@Override
	public final boolean canStore(MultipartFile multipartFile) {
		String ext = FileUtils.getFileExtension(multipartFile.getOriginalFilename());
		return ImageHelper.isSystemAllowedImage(ext);
	}

	@Override
	public String getUrl(String key) {
		if (sourceProtected) {
			if (ImageHelper.isGIF(FileUtils.getFileExtension(key))) {
				return super.getUrl(key);
			}
			Resize resize = largeResize == null ? (middleResize == null ? smallResize : middleResize) : largeResize;
			return buildResizePath(resize, key);
		} else {
			return super.getUrl(key);
		}
	}

	public void setSourceProtected(boolean sourceProtected) {
		this.sourceProtected = sourceProtected;
	}

	@Override
	protected Optional<Resource> handleOriginalFile(Path path) {
		String ext = FileUtils.getFileExtension(path);
		if (ImageHelper.isGIF(ext) || !sourceProtected) {
			return Optional.of(new PathResource(path));
		}
		return Optional.empty();
	}

	@Override
	protected void extraPoster(Path original, Path poster) {
		throw new SystemException("unaccepted !!!");
	}
}
