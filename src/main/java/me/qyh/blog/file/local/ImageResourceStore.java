package me.qyh.blog.file.local;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
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
import me.qyh.blog.message.Message;
import me.qyh.util.Webs;

public class ImageResourceStore extends AbstractLocalResourceRequestHandlerFileStore {

	private static final Logger logger = LoggerFactory.getLogger(ImageResourceStore.class);

	private static final Set<String> errorThumbPaths = new HashSet<String>();// 当缩略图制作失败时存放路径，防止下次再次读取

	@Autowired
	private ImageHelper imageHelper;
	@Autowired(required = false)
	private ResizeUrlParser resizeUrlParser;
	private boolean enableResize;

	private String thumbAbsPath;
	private File thumbAbsFolder;

	private Resize defaultResize;

	@Override
	public CommonFile store(MultipartFile mf) throws LogicException, IOException {
		CommonFile cf = super.store(mf);
		File file = getFile(cf.getKey());
		try {
			ImageInfo ii = imageHelper.read(file);
			cf.setWidth(ii.getWidth());
			cf.setHeight(ii.getHeight());
			return cf;
		} catch (ImageReadException e) {
			if (!FileUtils.deleteQuietly(file)) {
				logger.warn("无法删除文件:" + file);
				file.deleteOnExit();
			}
			throw new LogicException(new Message("image.corrupt", "不是正确的图片文件或者图片已经损坏"));
		}
	}

	@Override
	protected Resource getResource(String path, HttpServletRequest request) {
		Resource finalResource = null;
		// 如果允许缩放
		if (enableResize) {
			// 从链接中获取缩放信息
			Resize resize = resizeUrlParser.getResizeFromPath(path);
			if (resize != null) {
				if (errorThumbPaths.contains(path)) {
					return null;
				}
				// 缩略图是否已经存在
				File file = findThumbByPath(path);
				if (!file.exists()) {
					// 缩略图不存在，寻找原图
					LocalCommonFile cf = findSourceByPath(path, true);
					// 如果原图存在，进行缩放
					if (cf != null) {
						String extension = cf.getExtension();
						for (String imgExtension : ImageHelper.IMG_EXTENSIONS) {
							if (extension.equalsIgnoreCase(imgExtension)) {
								synchronized (cf) {
									File check = findThumbByPath(path);
									if (check.exists()) {
										return new PathResource(check.toPath());
									}
									try {
										return new PathResource(doResize(cf, resize, path).toPath());
									} catch (Exception e) {
										logger.error(e.getMessage(), e);
										errorThumbPaths.add(path);
										return null;
									}
								}
							}
						}
					}
				} else {
					return new PathResource(file.toPath());
				}
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
		return Webs.isImage(FilenameUtils.getExtension(multipartFile.getOriginalFilename()));
	}

	@Override
	public String getPreviewUrl(CommonFile cf) {
		if (!enableResize || defaultResize == null) {
			return getUrl(cf);
		} else {
			String path = cf.getKey();
			if (!path.startsWith("/")) {
				path = "/" + path;
			}
			return StringUtils.cleanPath(urlPrefix + resizeUrlParser.generateResizePathFromPath(defaultResize, path));
		}
	}

	@Override
	public void _afterPropertiesSet() throws Exception {
		if (thumbAbsPath == null) {
			throw new SystemException("缩略图存储路径不能为null");
		}
		thumbAbsFolder = new File(thumbAbsPath);
		FileUtils.forceMkdir(thumbAbsFolder);

		enableResize = (resizeUrlParser != null);

		if (enableResize && defaultResize != null && !resizeUrlParser.validResize(defaultResize)) {
			logger.warn("默认缩放尺寸：" + defaultResize + "无法被接受！请调整ResizeUrlParser");
			defaultResize = null;
		}
	}

	private File findThumbByPath(String path) {
		String southPath = resizeUrlParser.getSourcePathByResizePath(path);
		File thumbDir = new File(thumbAbsFolder, southPath);
		String name = FilenameUtils.getName(path);
		return new File(thumbDir, name);
	}

	private LocalCommonFile findSourceByPath(String path, boolean isThumb) {
		if (isThumb) {
			// 如果是缩略图，获取原图path
			path = resizeUrlParser.getSourcePathByResizePath(path);
		}
		return findByKey(path);
	}

	protected File doResize(LocalCommonFile cf, Resize resize, String path) throws Exception {
		File src = cf.getFile();
		ImageInfo ii = imageHelper.read(src);
		File cover = new File(thumbAbsFolder,
				cf.getKey() + File.separator + FilenameUtils.getBaseName(cf.getKey()) + "." + ImageHelper.JPEG);
		if (!cover.exists()) {
			FileUtils.forceMkdir(cover.getParentFile());
			if (ImageHelper.GIF.equalsIgnoreCase(ii.getExtension())) {
				imageHelper.getGifCover(src, cover);
			} else {
				imageHelper.format(src, cover);
			}
		}
		src = cover;
		File thumb = findThumbByPath(path);
		FileUtils.forceMkdir(thumb.getParentFile());
		imageHelper.resize(resize, src, thumb);
		return thumb;
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
}
