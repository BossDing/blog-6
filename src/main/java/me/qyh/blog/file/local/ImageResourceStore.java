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
import java.util.concurrent.CopyOnWriteArraySet;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import me.qyh.blog.exception.LogicException;
import me.qyh.blog.exception.SystemException;
import me.qyh.blog.file.CommonFile;
import me.qyh.blog.file.DefaultResizeValidator;
import me.qyh.blog.file.ImageHelper;
import me.qyh.blog.file.ImageHelper.ImageInfo;
import me.qyh.blog.file.Resize;
import me.qyh.blog.file.ResizeValidator;
import me.qyh.blog.file.ThumbnailUrl;

public class ImageResourceStore extends AbstractLocalResourceRequestHandlerFileStore {

	private static final Logger logger = LoggerFactory.getLogger(ImageResourceStore.class);
	private static final Set<String> errorThumbPaths = new CopyOnWriteArraySet<String>();// 当缩略图制作失败时存放路径，防止下次再次读取
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

	private boolean supportWebp = true;

	private String thumbAbsPath;
	private File thumbAbsFolder;

	private Resize smallResize;
	private Resize middleResize;
	private Resize largeResize;

	@Override
	public CommonFile store(String key, MultipartFile mf) throws LogicException, IOException {
		File dest = new File(absFolder, key);
		if (dest.exists()) {
			throw new LogicException("file.local.exists", "文件" + dest.getAbsolutePath() + "已经存在",
					dest.getAbsolutePath());
		}
		if (inThumbDir(dest)) {
			throw new LogicException("file.inThumb", "文件" + dest.getAbsolutePath() + "不能被存放在缩略图文件夹下",
					dest.getAbsolutePath());
		}
		// 先写入临时文件
		File tmp = File.createTempFile(RandomStringUtils.randomNumeric(6),
				"." + FilenameUtils.getExtension(mf.getOriginalFilename()));
		mf.transferTo(tmp);
		File finalFile = tmp;
		try {
			ImageInfo ii = imageHelper.read(tmp);
			String extension = ii.getExtension();
			if (ImageHelper.isWEBP(extension)) {
				if (!supportWebp) {
					throw new LogicException("file.format.notsupport", "webp格式不被支持", "webp");
				}
				// 如果是webp的图片，需要转化为jpeg格式
				finalFile = File.createTempFile(RandomStringUtils.randomNumeric(7), "." + ImageHelper.JPEG);
				try {
					imageHelper.format(tmp, finalFile);
				} catch (Exception e) {
					throw new SystemException(e.getMessage(), e);
				}
				extension = ImageHelper.JPEG;
			}

			FileUtils.forceMkdir(dest.getParentFile());
			FileUtils.moveFile(finalFile, dest);

			CommonFile cf = new CommonFile();
			cf.setExtension(extension);
			cf.setSize(mf.getSize());
			cf.setStore(id);
			cf.setOriginalFilename(mf.getOriginalFilename());

			cf.setWidth(ii.getWidth());
			cf.setHeight(ii.getHeight());

			return cf;
		} catch (ImageReadException e) {
			throw new LogicException("image.corrupt", "不是正确的图片文件或者图片已经损坏");
		} finally {
			if (finalFile.exists() && !FileUtils.deleteQuietly(finalFile)) {
				finalFile.deleteOnExit();
			}
			if (tmp.exists() && !FileUtils.deleteQuietly(tmp)) {
				tmp.deleteOnExit();
			}
		}
	}

	@Override
	protected Resource getResource(String path, HttpServletRequest request) {
		// 从链接中获取缩放信息
		Resize resize = getResizeFromPath(path);
		if (sourceProtected && resize == null) {
			String ext = FilenameUtils.getExtension(path);
			// 如果是GIF图片,直接输出原图
			if (!ImageHelper.isGIF(ext)) {
				return null;
			}
		}
		Resource finalResource = null;
		boolean supportWebp = supportWebp(request);
		if (resize != null) {
			String ext = FilenameUtils.getExtension(getSourcePathByResizePath(path));
			String thumbPath = path + (supportWebp ? WEBP_EXT
					: (ImageHelper.isGIF(ext) || ImageHelper.isPNG(ext)) ? PNG_EXT : JPEG_EXT);
			if (errorThumbPaths.contains(thumbPath)) {
				return null;
			}
			// 缩略图是否已经存在
			File file = findThumbByPath(thumbPath);
			if (!file.exists()) {
				String sourcePath = getSourcePathByResizePath(path);
				// 缩略图不存在，寻找原图
				File local = super.findByKey(sourcePath);
				// 如果原图存在，进行缩放
				if (local != null) {
					if (ImageHelper.isImage(FilenameUtils.getExtension(local.getName()))) {
						synchronized (this) {
							File check = findThumbByPath(thumbPath);
							if (check.exists()) {
								return new PathResource(check.toPath());
							}
							try {
								return new PathResource(doResize(local, resize, check, sourcePath).toPath());
							} catch (Throwable e) {
								logger.error(e.getMessage(), e);
								errorThumbPaths.add(path);
								return null;
							}
						}
					}
				}
			} else {
				return new PathResource(file.toPath());
			}
		}
		if (finalResource == null) {
			// 寻找源文件
			File local = super.findByKey(path);
			if (local != null) {
				finalResource = new PathResource(local.toPath());
			}
		}
		return finalResource;
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
		return ImageHelper.isImage(FilenameUtils.getExtension(multipartFile.getOriginalFilename()));
	}

	@Override
	public String getUrl(String key) {
		if (sourceProtected) {
			if (ImageHelper.isGIF(FilenameUtils.getExtension(key))) {
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
		if (!key.startsWith("/"))
			key = "/" + key;
		if (resize == null)
			return getUrl(key);

		return StringUtils.cleanPath(urlPrefix + generateResizePathFromPath(resize, key));
	}

	@Override
	public void _afterPropertiesSet() throws Exception {
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
		if (resize != null && !resizeValidator.valid(resize))
			throw new SystemException("默认缩放尺寸：" + resize + "无法被接受！请调整ResizeUrlParser");
	}

	private File findThumbByPath(String path) {
		String southPath = getSourcePathByResizePath(path);
		File thumbDir = new File(thumbAbsFolder, southPath);
		String name = FilenameUtils.getName(path);
		return new File(thumbDir, name);
	}

	protected File doResize(File local, Resize resize, File thumb, String key) throws Exception {
		ImageInfo ii = imageHelper.read(local);
		// 不知道为什么。gm转化webp的时候特别耗费时间，所以这里只提取jpeg|PNG的封面
		String ext = FilenameUtils.getExtension(local.getName());
		File cover = new File(thumbAbsFolder, key + File.separator + FilenameUtils.getBaseName(key)
				+ (ImageHelper.isGIF(ext) || ImageHelper.isPNG(ext) ? PNG_EXT : JPEG_EXT));
		if (!cover.exists()) {
			FileUtils.forceMkdir(cover.getParentFile());
			if (ImageHelper.isGIF(ii.getExtension())) {
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
		if (!supportWebp)
			return false;
		if (request.getParameter(NO_WEBP) != null)
			return false;
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
		String extension = FilenameUtils.getExtension(path);
		if (extension != null && !extension.isEmpty()) {
			return null;
		}
		Resize resize;
		String baseName = FilenameUtils.getBaseName(path);
		if (baseName.indexOf(CONCAT_CHAR) != -1) {
			boolean keepRatio = true;
			if (baseName.endsWith(Character.toString(FORCE_CHAR))) {
				keepRatio = false;
				baseName = baseName.substring(0, baseName.length() - 1);
			}
			if (baseName.startsWith(Character.toString(CONCAT_CHAR))) {
				try {
					baseName = baseName.substring(1, baseName.length());
					Integer h = Integer.valueOf(baseName);
					resize = new Resize();
					resize.setHeight(h);
					resize.setKeepRatio(keepRatio);
				} catch (NumberFormatException e) {
					return null;
				}
			} else if (baseName.endsWith(Character.toString(CONCAT_CHAR))) {
				try {
					baseName = baseName.substring(0, baseName.length() - 1);
					Integer w = Integer.valueOf(baseName);
					resize = new Resize();
					resize.setWidth(w);
					resize.setKeepRatio(keepRatio);
				} catch (NumberFormatException e) {
					return null;
				}
			} else {
				String[] splits = baseName.split(Character.toString(CONCAT_CHAR));
				if (splits.length != 2) {
					return null;
				} else {
					try {
						Integer w = Integer.valueOf(splits[0]);
						Integer h = Integer.valueOf(splits[1]);
						resize = new Resize(w, h, keepRatio);
					} catch (NumberFormatException e) {
						return null;
					}
				}
			}

		} else {
			try {
				resize = new Resize(Integer.valueOf(baseName));
			} catch (NumberFormatException e) {
				return null;
			}
		}
		return resize != null ? (resizeValidator.valid(resize) ? resize : null) : null;
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
		if (idOf != -1)
			return path.substring(0, path.lastIndexOf('/'));
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
			return FileUtils.directoryContains(thumbAbsFolder, dest);
		} catch (IOException e) {
			throw new SystemException(e.getMessage(), e);
		}
	}

	/**
	 * 是有要缩放
	 * 
	 * @param resize
	 *            要缩放的尺寸
	 * @param width
	 *            原图宽
	 * @param height
	 *            原图高
	 * @return
	 */
	// protected boolean needResize(Resize resize, int width, int height) {
	// if (resize.getSize() != null) {
	// return (width >= resize.getSize() || height >= resize.getHeight())
	// && !(width == height && width == resize.getSize());
	// }
	// return width >= resize.getWidth() && height >= resize.getHeight()
	// && !(width == resize.getWidth() && height == resize.getHeight());
	// }

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
