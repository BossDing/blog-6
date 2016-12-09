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
package me.qyh.blog.file.local;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.google.common.collect.Sets;
import com.google.common.io.Files;

import me.qyh.blog.exception.LogicException;
import me.qyh.blog.exception.SystemException;
import me.qyh.blog.file.CommonFile;
import me.qyh.blog.file.DefaultResizeValidator;
import me.qyh.blog.file.ImageHelper;
import me.qyh.blog.file.ImageHelper.ImageInfo;
import me.qyh.blog.file.Resize;
import me.qyh.blog.file.ResizeValidator;
import me.qyh.blog.file.ThumbnailUrl;
import me.qyh.blog.util.FileUtils;
import me.qyh.blog.web.Webs;

/**
 * 本地图片存储，图片访问
 * 
 * @author Administrator
 *
 */
public class ImageResourceStore extends AbstractLocalResourceRequestHandlerFileStore {

	private static final Logger imageResourceStoreLogger = LoggerFactory.getLogger(ImageResourceStore.class);
	private static final Set<String> errorThumbPaths = Sets.newHashSet();// 当缩略图制作失败时存放路径，防止下次再次读取
	private static final String WEBP_ACCEPT = "image/webp";
	private static final String WEBP_EXT = ".webp";
	private static final String JPEG_EXT = ".jpeg";
	private static final String PNG_EXT = ".png";
	private static final char CONCAT_CHAR = 'X';
	private static final char FORCE_CHAR = '!';

	private static final String NO_WEBP = "nowebp";

	private ResizeValidator resizeValidator;

	@Autowired
	private ImageHelper imageHelper;

	/**
	 * 原图保护
	 */
	private boolean sourceProtected;

	private boolean supportWebp = false;

	private String thumbAbsPath;
	private File thumbAbsFolder;

	private Resize smallResize;
	private Resize middleResize;
	private Resize largeResize;

	@Override
	public CommonFile store(String key, MultipartFile mf) throws LogicException {
		File dest = new File(absFolder, key);
		checkFileStoreable(dest);
		// 先写入临时文件
		String originalFilename = mf.getOriginalFilename();
		File tmp = FileUtils.temp(Files.getFileExtension(originalFilename));
		try {
			Webs.save(mf, tmp);
		} catch (IOException e1) {
			throw new SystemException(e1.getMessage(), e1);
		}
		File finalFile = tmp;
		ImageInfo ii = readImage(tmp);
		String extension = ii.getExtension();
		if (ImageHelper.isWEBP(extension)) {
			finalFile = FileUtils.temp(ImageHelper.JPEG);
			webpFormat(tmp, finalFile);
			extension = ImageHelper.JPEG;
		}
		try {
			FileUtils.forceMkdir(dest.getParentFile());
			Files.move(finalFile, dest);
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
			FileUtils.deleteQuietly(tmp);
		}
	}

	private void checkFileStoreable(File dest) throws LogicException {
		if (dest.exists()) {
			throw new LogicException("file.local.exists", "文件" + dest.getAbsolutePath() + "已经存在",
					dest.getAbsolutePath());
		}
		if (inThumbDir(dest)) {
			throw new LogicException("file.inThumb", "文件" + dest.getAbsolutePath() + "不能被存放在缩略图文件夹下",
					dest.getAbsolutePath());
		}
	}

	private ImageInfo readImage(File tmp) throws LogicException {
		try {
			return imageHelper.read(tmp);
		} catch (IOException e) {
			imageResourceStoreLogger.debug(e.getMessage(), e);
			throw new LogicException("image.corrupt", "不是正确的图片文件或者图片已经损坏");
		}
	}

	private void webpFormat(File src, File dest) throws LogicException {
		if (!supportWebp) {
			throw new LogicException("file.format.notsupport", "webp格式不被支持", "webp");
		}
		// 如果是webp的图片，需要转化为jpeg格式
		try {
			imageHelper.format(src, dest);
		} catch (IOException e) {
			throw new SystemException(e.getMessage(), e);
		}
	}

	@Override
	protected Resource getResource(String path, HttpServletRequest request) {
		// 从链接中获取缩放信息
		Resize resize = getResizeFromPath(path);
		if (sourceProtected && resize == null) {
			String ext = Files.getFileExtension(path);
			// 如果是GIF图片,直接输出原图
			if (!ImageHelper.isGIF(ext)) {
				return null;
			}
		}
		boolean detectSupportWebp = supportWebp(request);
		if (resize != null) {
			String ext = Files.getFileExtension(getSourcePathByResizePath(path));
			String thumbPath = path + (detectSupportWebp ? WEBP_EXT
					: (ImageHelper.isGIF(ext) || ImageHelper.isPNG(ext)) ? PNG_EXT : JPEG_EXT);
			// 缩略图是否已经存在
			File file = findThumbByPath(thumbPath);
			if (!file.exists()) {
				String sourcePath = getSourcePathByResizePath(path);
				// 缩略图不存在，寻找原图
				File local = super.findByKey(sourcePath);
				if (local == null) {
					// 返回null
					return null;
				}
				// 如果原图存在，进行缩放
				if (errorThumbPaths.contains(thumbPath)) {
					return detectSupportWebp ? null : new PathResource(local.toPath());
				}
				if (imageHelper.supportFormat(Files.getFileExtension(local.getName()))) {
					synchronized (this) {
						File check = findThumbByPath(thumbPath);
						if (check.exists()) {
							return new PathResource(check.toPath());
						}
						try {
							return new PathResource(doResize(local, resize, check, sourcePath).toPath());
						} catch (IOException e) {
							imageResourceStoreLogger.error(e.getMessage(), e);
							errorThumbPaths.add(thumbPath);
							return detectSupportWebp ? null : new PathResource(local.toPath());
						}
					}
				}
			} else {
				return new PathResource(file.toPath());
			}
		}
		// 寻找源文件
		File local = super.findByKey(path);
		if (local != null) {
			return new PathResource(local.toPath());
		}
		return null;
	}

	@Override
	public boolean delete(String key) {
		boolean flag = super.delete(key);
		if (flag) {
			File thumbDir = new File(thumbAbsFolder, key);
			if (thumbDir.exists()) {
				flag = FileUtils.deleteQuietly(thumbDir);
			}
		}
		return flag;
	}

	@Override
	public boolean deleteBatch(String key) {
		return delete(key);
	}

	@Override
	public boolean canStore(MultipartFile multipartFile) {
		return imageHelper.supportFormat(Files.getFileExtension(multipartFile.getOriginalFilename()));
	}

	@Override
	public String getUrl(String key) {
		if (sourceProtected) {
			if (ImageHelper.isGIF(Files.getFileExtension(key))) {
				return super.getUrl(key);
			}
			Resize resize = largeResize == null ? (middleResize == null ? smallResize : middleResize) : largeResize;
			return buildResizePath(resize, key);
		} else {
			return super.getUrl(key);
		}
	}

	@Override
	public ThumbnailUrl getThumbnailUrl(String key) {
		return new ThumbnailUrl(buildResizePath(smallResize, key), buildResizePath(middleResize, key),
				buildResizePath(largeResize, key));
	}

	private String buildResizePath(Resize resize, String key) {
		String path = key;
		if (!key.startsWith("/")) {
			path = "/" + key;
		}
		if (resize == null) {
			return getUrl(path);
		}
		return StringUtils.cleanPath(urlPrefix + generateResizePathFromPath(resize, path));
	}

	@Override
	public void moreAfterPropertiesSet() {
		if (thumbAbsPath == null) {
			throw new SystemException("缩略图存储路径不能为null");
		}
		thumbAbsFolder = new File(thumbAbsPath);
		FileUtils.forceMkdir(thumbAbsFolder);

		if (resizeValidator == null) {
			resizeValidator = new DefaultResizeValidator();
		}

		validateResize(smallResize);
		validateResize(middleResize);
		validateResize(largeResize);

		if (sourceProtected && (smallResize == null && middleResize == null && largeResize == null)) {
			throw new SystemException("开启原图保护必须提供默认缩放尺寸");
		}

		if (sourceProtected) {
			setEnableDownloadHandler(false);
		}
	}

	private void validateResize(Resize resize) {
		if (resize != null && !resizeValidator.valid(resize)) {
			throw new SystemException("默认缩放尺寸：" + resize + "无法被接受！请调整ResizeUrlParser");
		}
	}

	private File findThumbByPath(String path) {
		String southPath = getSourcePathByResizePath(path);
		File thumbDir = new File(thumbAbsFolder, southPath);
		String name = new File(path).getName();
		return new File(thumbDir, name);
	}

	protected File doResize(File local, Resize resize, File thumb, String key) throws IOException {
		// 不知道为什么。gm转化webp的时候特别耗费时间，所以这里只提取jpeg|PNG的封面
		String ext = Files.getFileExtension(local.getName());
		File cover = new File(thumbAbsFolder, key + File.separator + Files.getNameWithoutExtension(key)
				+ (ImageHelper.isGIF(ext) || ImageHelper.isPNG(ext) ? PNG_EXT : JPEG_EXT));
		if (!cover.exists()) {
			FileUtils.forceMkdir(cover.getParentFile());
			if (ImageHelper.isGIF(Files.getFileExtension(local.getName()))) {
				imageHelper.getGifCover(local, cover);
			} else {
				imageHelper.format(local, cover);
			}
		}
		FileUtils.forceMkdir(thumb.getParentFile());
		// 基于封面缩放
		imageHelper.resize(resize, cover, thumb);
		return thumb;
	}

	protected boolean supportWebp(HttpServletRequest request) {
		if (!supportWebp) {
			return false;
		}
		if (request.getParameter(NO_WEBP) != null) {
			return false;
		}
		String accept = request.getHeader("Accept");
		if (accept != null && accept.indexOf(WEBP_ACCEPT) != -1) {
			return true;
		}
		return false;
	}

	protected String generateResizePathFromPath(Resize resize, String path) {
		if (!resizeValidator.valid(resize)) {
			return path;
		}
		StringBuilder sb = new StringBuilder("/");
		sb.append(getThumname(resize));
		return StringUtils.cleanPath(path + sb.toString());
	}

	protected Resize getResizeFromPath(String path) {
		String extension = Files.getFileExtension(path);
		if (extension != null && !extension.isEmpty()) {
			return null;
		}
		Resize resize = null;
		String baseName = Files.getNameWithoutExtension(path);
		try {
			if (baseName.indexOf(CONCAT_CHAR) != -1) {
				boolean keepRatio = true;
				if (baseName.endsWith(Character.toString(FORCE_CHAR))) {
					keepRatio = false;
					baseName = baseName.substring(0, baseName.length() - 1);
				}
				if (baseName.startsWith(Character.toString(CONCAT_CHAR))) {
					baseName = baseName.substring(1, baseName.length());
					Integer h = Integer.valueOf(baseName);
					resize = new Resize();
					resize.setHeight(h);
					resize.setKeepRatio(keepRatio);
				} else if (baseName.endsWith(Character.toString(CONCAT_CHAR))) {
					baseName = baseName.substring(0, baseName.length() - 1);
					Integer w = Integer.valueOf(baseName);
					resize = new Resize();
					resize.setWidth(w);
					resize.setKeepRatio(keepRatio);
				} else {
					String[] splits = baseName.split(Character.toString(CONCAT_CHAR));
					if (splits.length != 2) {
						return null;
					} else {
						Integer w = Integer.valueOf(splits[0]);
						Integer h = Integer.valueOf(splits[1]);
						resize = new Resize(w, h, keepRatio);
					}
				}
			} else {
				resize = new Resize(Integer.valueOf(baseName));
			}
		} catch (NumberFormatException e) {
			return null;
		}
		return resizeValidator.valid(resize) ? resize : null;
	}

	private String getThumname(Resize resize) {
		StringBuilder sb = new StringBuilder();
		if (resize.getSize() != null) {
			sb.append(resize.getSize());
		} else {
			sb.append((resize.getWidth() <= 0) ? "" : resize.getWidth());
			sb.append(CONCAT_CHAR);
			sb.append(resize.getHeight() <= 0 ? "" : resize.getHeight());
			sb.append(resize.isKeepRatio() ? "" : FORCE_CHAR);
		}
		return sb.toString();
	}

	protected String getSourcePathByResizePath(String path) {
		int idOf = path.lastIndexOf('/');
		if (idOf != -1) {
			return path.substring(0, path.lastIndexOf('/'));
		}
		return path;
	}

	/**
	 * 要创建的文件是否在缩略图文件夹中
	 * 
	 * @param dest
	 * @return
	 */
	private boolean inThumbDir(File dest) {
		try {
			String canonicalP = thumbAbsFolder.getCanonicalPath();
			String canonicalC = dest.getCanonicalPath();
			return canonicalP.equals(canonicalC)
					|| canonicalC.regionMatches(false, 0, canonicalP, 0, canonicalP.length());
		} catch (IOException e) {
			throw new SystemException(e.getMessage(), e);
		}
	}

	public void setThumbAbsPath(String thumbAbsPath) {
		this.thumbAbsPath = thumbAbsPath;
	}

	public void setImageHelper(ImageHelper imageHelper) {
		this.imageHelper = imageHelper;
	}

	public void setSourceProtected(boolean sourceProtected) {
		this.sourceProtected = sourceProtected;
	}

	public void setResizeValidator(ResizeValidator resizeValidator) {
		this.resizeValidator = resizeValidator;
	}

	public void setSupportWebp(boolean supportWebp) {
		this.supportWebp = supportWebp;
	}

	public void setSmallResize(Resize smallResize) {
		this.smallResize = smallResize;
	}

	public void setMiddleResize(Resize middleResize) {
		this.middleResize = middleResize;
	}

	public void setLargeResize(Resize largeResize) {
		this.largeResize = largeResize;
	}
}
