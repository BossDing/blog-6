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
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

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
import me.qyh.blog.file.ImageHelper;
import me.qyh.blog.file.ImageHelper.ImageInfo;
import me.qyh.blog.file.Resize;
import me.qyh.blog.file.ResizeValidator;
import me.qyh.blog.file.ThumbnailUrl;
import me.qyh.blog.util.FileUtils;
import me.qyh.blog.util.Validators;
import me.qyh.blog.web.Webs;

/**
 * 本地图片存储，图片访问
 * 
 * @author Administrator
 *
 */
public class ImageResourceStore extends AbstractLocalResourceRequestHandlerFileStore {
	private static final Logger IMG_RESOURCE_LOGGER = LoggerFactory.getLogger(ImageResourceStore.class);
	private static final Set<String> errorThumbPaths = new HashSet<>();// 当缩略图制作失败时存放路径，防止下次再次读取
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

	public ImageResourceStore(String urlPatternPrefix) {
		super(urlPatternPrefix);
	}

	public ImageResourceStore() {
		super("image");
	}

	@Override
	public CommonFile store(String key, MultipartFile mf) throws LogicException {
		File dest = new File(absFolder, key);
		checkFileStoreable(dest);
		// 先写入临时文件
		String originalFilename = mf.getOriginalFilename();
		File tmp = FileUtils.temp(FileUtils.getFileExtension(originalFilename));
		try {
			Webs.save(mf, tmp);
		} catch (IOException e1) {
			FileUtils.deleteQuietly(tmp);
			throw new SystemException(e1.getMessage(), e1);
		}
		File finalFile = tmp;
		try {
			ImageInfo ii = readImage(tmp);
			String extension = ii.getExtension();
			FileUtils.forceMkdir(dest.getParentFile());
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
			FileUtils.deleteQuietly(tmp);
		}
	}

	private void checkFileStoreable(File dest) throws LogicException {
		if (dest.exists() && !FileUtils.deleteQuietly(dest)) {
			throw new LogicException("file.store.exists", "文件" + dest.getAbsolutePath() + "已经存在",
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
			IMG_RESOURCE_LOGGER.debug(e.getMessage(), e);
			throw new LogicException("image.corrupt", "不是正确的图片文件或者图片已经损坏");
		}
	}

	@Override
	protected Optional<Resource> getResource(String path, HttpServletRequest request) {
		// 从链接中获取缩放信息
		Optional<Resize> optionalResize = getResizeFromPath(path);
		if (sourceProtected && !optionalResize.isPresent()) {
			String ext = FileUtils.getFileExtension(path);
			// 如果是GIF图片,直接输出原图
			if (!ImageHelper.isGIF(ext)) {
				return Optional.empty();
			}
		}
		boolean detectSupportWebp = supportWebp(request);
		if (optionalResize.isPresent()) {
			Resize resize = optionalResize.get();
			String ext = FileUtils.getFileExtension(getSourcePathByResizePath(path));
			String thumbPath = path + (detectSupportWebp ? WEBP_EXT
					: (ImageHelper.isGIF(ext) || ImageHelper.isPNG(ext)) ? PNG_EXT : JPEG_EXT);
			// 缩略图是否已经存在
			File file = findThumbByPath(thumbPath);
			// 缩略图不存在，寻找原图
			if (!file.exists()) {
				String sourcePath = getSourcePathByResizePath(path);
				Optional<File> optionalFile = super.findByKey(sourcePath);
				if (!optionalFile.isPresent()) {
					// 源文件也不存在
					return Optional.empty();
				}
				// 如果原图存在，进行缩放
				File local = optionalFile.get();
				// 如果已经缩放失败过，直接返回原图
				if (errorThumbPaths.contains(thumbPath)) {
					return Optional.of(new PathResource(local.toPath()));
				}
				// 如果支持文件格式(防止ImageHelper变更)
				if (ImageHelper.isSystemAllowedImage(FileUtils.getFileExtension(local.getName()))) {
					try {
						return Optional.of(new PathResource(doResize(local, resize, file, sourcePath).toPath()));
					} catch (IOException e) {
						IMG_RESOURCE_LOGGER.error(e.getMessage(), e);
						errorThumbPaths.add(thumbPath);
						return Optional.of(new PathResource(local.toPath()));
					}
				} else {
					// 不支持的文件格式
					// 可能更改了ImageHelper
					// 这里直接输出源文件
					return Optional.of(new PathResource(local.toPath()));
				}

			} else {
				// 直接返回缩略图
				return Optional.of(new PathResource(file.toPath()));
			}
		}
		// 寻找源文件
		return findByKey(path).map(file -> new PathResource(file.toPath()));
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

	@Override
	public Optional<ThumbnailUrl> getThumbnailUrl(String key) {
		return Optional.of(new ThumbnailUrl(buildResizePath(smallResize, key), buildResizePath(middleResize, key),
				buildResizePath(largeResize, key)));
	}

	private String buildResizePath(Resize resize, String key) {
		String path = key;
		if (!key.startsWith("/")) {
			path = "/" + key;
		}
		if (resize == null) {
			return getUrl(path);
		}
		return urlPrefix + Validators.cleanPath(generateResizePathFromPath(resize, path));
	}

	@Override
	public void moreAfterPropertiesSet() {
		if (thumbAbsPath == null) {
			throw new SystemException("缩略图存储路径不能为null");
		}
		thumbAbsFolder = new File(thumbAbsPath);
		FileUtils.forceMkdir(thumbAbsFolder);

		if (resizeValidator == null) {
			resizeValidator = resize -> true;
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

		if (!imageHelper.supportWebp()) {
			supportWebp = false;
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
		String ext = FileUtils.getFileExtension(local.getName());
		File thumbFolder = new File(thumbAbsFolder, key);

		String thumbFilename = FileUtils.getNameWithoutExtension(key);
		String coverExt = (ImageHelper.isGIF(ext) || ImageHelper.isPNG(ext) ? PNG_EXT : JPEG_EXT);

		File cover = new File(thumbFolder, thumbFilename + ".cover" + coverExt);
		if (!cover.exists()) {
			// 这里不直接写入cover文件
			// 因为在使用GraphicsMagick的情况下，会先生成一个空的文件
			// 如果两个线程同时生成缩略图，其中一个线程可能会读取到空的封面
			// 所以这里先写入再重命名，确保cover是一个被写入完毕的文件
			File target = new File(thumbFolder, thumbFilename + coverExt);
			FileUtils.forceMkdir(thumbFolder);
			if (ImageHelper.isGIF(FileUtils.getFileExtension(local.getName()))) {
				imageHelper.getGifCover(local, target);
			} else {
				imageHelper.format(local, target);
			}

			FileUtils.rename(target, cover);
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

	protected Optional<Resize> getResizeFromPath(String path) {
		String extension = FileUtils.getFileExtension(path);
		if (!extension.isEmpty()) {
			return Optional.empty();
		}
		Resize resize = null;
		String baseName = FileUtils.getNameWithoutExtension(path);
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
						return Optional.empty();
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
			return Optional.empty();
		}
		return resizeValidator.valid(resize) ? Optional.of(resize) : Optional.empty();
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
