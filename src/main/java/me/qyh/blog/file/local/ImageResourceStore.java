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

public class ImageResourceStore extends AbstractLocalResourceRequestHandlerFileStore {

	private static final Logger logger = LoggerFactory.getLogger(ImageResourceStore.class);
	private static final Set<String> errorThumbPaths = new CopyOnWriteArraySet<String>();// 当缩略图制作失败时存放路径，防止下次再次读取
	private static final String WEBP_ACCEPT = "image/webp";
	private static final String WEBP_EXT = ".webp";
	private static final String JPEG_EXT = ".jpeg";
	private static final char CONCAT_CHAR = 'X';
	private static final char FORCE_CHAR = '!';

	private ResizeValidator resizeValidator;

	@Autowired
	private ImageHelper imageHelper;

	/**
	 * 原图保护
	 */
	private boolean sourceProtected;

	private boolean enableWebp = true;

	private String thumbAbsPath;
	private File thumbAbsFolder;

	private Resize defaultResize;

	@Override
	public CommonFile store(MultipartFile mf) throws LogicException, IOException {
		// 先写入临时文件
		File tmp = File.createTempFile(RandomStringUtils.randomNumeric(6),
				"." + FilenameUtils.getExtension(mf.getOriginalFilename()));
		mf.transferTo(tmp);
		File finalFile = tmp;
		try {
			ImageInfo ii = imageHelper.read(tmp);
			String extension = ii.getExtension();
			if (ImageHelper.isWEBP(extension)) {
				// 如果是webp的图片，需要转化为jpeg格式
				finalFile = File.createTempFile(RandomStringUtils.randomNumeric(7), "." + ImageHelper.JPEG);
				try {
					imageHelper.format(tmp, finalFile);
				} catch (Exception e) {
					throw new SystemException(e.getMessage(), e);
				}
				extension = ImageHelper.JPEG;
			}

			String newName = System.currentTimeMillis() + RandomStringUtils.randomNumeric(6) + "." + extension;
			String _folder = getStoreFolder();
			File folder = new File(new File(absPath), _folder);
			FileUtils.forceMkdir(folder);
			FileUtils.moveFile(finalFile, new File(folder, newName));

			CommonFile cf = new CommonFile();
			cf.setExtension(extension);
			cf.setSize(mf.getSize());
			cf.setKey(StringUtils.cleanPath(_folder) + "/" + newName);
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
		boolean supportWebp = enableWebp && supportWebp(request);
		if (resize != null) {
			String thumbPath = supportWebp ? path + WEBP_EXT : path + JPEG_EXT;
			if (errorThumbPaths.contains(thumbPath)) {
				return null;
			}
			// 缩略图是否已经存在
			File file = findThumbByPath(thumbPath);
			if (!file.exists()) {
				// 缩略图不存在，寻找原图
				LocalCommonFile cf = findSourceByPath(path, true);
				// 如果原图存在，进行缩放
				if (cf != null) {
					String extension = cf.getExtension();
					if (ImageHelper.isImage(extension)) {
						synchronized (cf) {
							File check = findThumbByPath(thumbPath);
							if (check.exists()) {
								return new PathResource(check.toPath());
							}
							try {
								return new PathResource(doResize(cf, resize, check).toPath());
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
			LocalCommonFile file = findSourceByPath(path, false);
			if (file != null) {
				finalResource = new PathResource(file.getFile().toPath());
			}
		}
		return finalResource;
	}

	@Override
	public boolean delete(CommonFile t) {
		boolean flag = super.delete(t);
		if (flag) {
			File thumbDir = new File(thumbAbsFolder, t.getKey());
			if (thumbDir.exists()) {
				flag = FileUtils.deleteQuietly(thumbDir);
			}
		}
		return flag;
	}

	@Override
	public boolean canStore(MultipartFile multipartFile) {
		return ImageHelper.isImage(FilenameUtils.getExtension(multipartFile.getOriginalFilename()));
	}

	@Override
	public String getUrl(CommonFile cf) {
		if (sourceProtected) {
			if (ImageHelper.isGIF(FilenameUtils.getExtension(cf.getKey()))) {
				return super.getUrl(cf);
			}
			return buildResizePath(cf);
		} else {
			return super.getUrl(cf);
		}
	}

	@Override
	public String getPreviewUrl(CommonFile cf) {
		if (defaultResize == null) {
			return getUrl(cf);
		} else {
			return buildResizePath(cf);
		}
	}

	private String buildResizePath(CommonFile cf) {
		String path = cf.getKey();
		if (!path.startsWith("/")) {
			path = "/" + path;
		}
		return StringUtils.cleanPath(urlPrefix + generateResizePathFromPath(defaultResize, path));
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

		if (defaultResize != null && !resizeValidator.valid(defaultResize)) {
			throw new SystemException("默认缩放尺寸：" + defaultResize + "无法被接受！请调整ResizeUrlParser");
		}

		if (sourceProtected && defaultResize == null) {
			throw new SystemException("开启原图保护必须提供默认缩放尺寸");
		}

		if (sourceProtected) {
			setEnableDownloadHandler(false);
		}
	}

	private File findThumbByPath(String path) {
		String southPath = getSourcePathByResizePath(path);
		File thumbDir = new File(thumbAbsFolder, southPath);
		String name = FilenameUtils.getName(path);
		return new File(thumbDir, name);
	}

	private LocalCommonFile findSourceByPath(String path, boolean isThumb) {
		if (isThumb) {
			// 如果是缩略图，获取原图path
			path = getSourcePathByResizePath(path);
		}
		return findByKey(path);
	}

	protected File doResize(LocalCommonFile cf, Resize resize, File thumb) throws Exception {
		File src = cf.getFile();
		ImageInfo ii = imageHelper.read(src);
		String ext = FilenameUtils.getExtension(thumb.getName());
		boolean needResize = needResize(resize, ii.getWidth(), ii.getHeight());
		// 将图片转为jpeg|webp格式，保持大小一致(获取图片封面)
		File cover = new File(thumbAbsFolder, cf.getKey() + File.separator + FilenameUtils.getBaseName(cf.getKey())
				+ (ImageHelper.WEBP.equalsIgnoreCase(ext) ? WEBP_EXT : JPEG_EXT));
		if (!cover.exists()) {
			FileUtils.forceMkdir(cover.getParentFile());
			if (ImageHelper.isGIF(ii.getExtension())) {
				imageHelper.getGifCover(src, cover);
			} else {
				imageHelper.format(src, cover);
			}
		}
		// 如果不需要缩放
		if (!needResize) {
			FileUtils.copyFile(cover, thumb);
		} else {
			FileUtils.forceMkdir(thumb.getParentFile());
			// 基于封面缩放
			imageHelper.resize(resize, cover, thumb);
		}
		return thumb;
	}

	protected boolean supportWebp(HttpServletRequest request) {
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
	protected boolean needResize(Resize resize, int width, int height) {
		if (resize.getSize() != null) {
			return (width >= resize.getSize() || height >= resize.getHeight())
					&& !(width == height && width == resize.getSize());
		}
		return width >= resize.getWidth() && height >= resize.getHeight()
				&& !(width == resize.getWidth() && height == resize.getHeight());
	}

	public void setThumbAbsPath(String thumbAbsPath) {
		this.thumbAbsPath = thumbAbsPath;
	}

	public void setImageHelper(ImageHelper imageHelper) {
		this.imageHelper = imageHelper;
	}

	public void setDefaultResize(Resize defaultResize) {
		this.defaultResize = defaultResize;
	}

	public void setSourceProtected(boolean sourceProtected) {
		this.sourceProtected = sourceProtected;
	}

	public void setResizeValidator(ResizeValidator resizeValidator) {
		this.resizeValidator = resizeValidator;
	}

	public void setEnableWebp(boolean enableWebp) {
		this.enableWebp = enableWebp;
	}

}
