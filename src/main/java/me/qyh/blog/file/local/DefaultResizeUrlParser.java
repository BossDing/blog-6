package me.qyh.blog.file.local;

import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.StringUtils;

import me.qyh.blog.file.DefaultResizeValidator;
import me.qyh.blog.file.ImageHelper;
import me.qyh.blog.file.Resize;
import me.qyh.blog.file.ResizeValidator;

public class DefaultResizeUrlParser implements ResizeUrlParser, InitializingBean {

	private static final char CONCAT_CHAR = 'X';
	private static final char FORCE_CHAR = '!';

	private ResizeValidator resizeValidator;

	@Override
	public String generateResizePathFromPath(Resize resize, String path) {
		if (!resizeValidator.valid(resize)) {
			return path;
		}
		StringBuilder sb = new StringBuilder("/");
		sb.append(getThumname(resize));
		return StringUtils.cleanPath(path + sb.toString());
	}

	@Override
	public Resize getResizeFromPath(String path) {
		String extension = FilenameUtils.getExtension(path);
		if (!ImageHelper.JPEG.equalsIgnoreCase(extension)) {
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
			sb.append(resize.getSize()).append(".").append(ImageHelper.JPEG);
		} else {
			sb.append((resize.getWidth() <= 0) ? "" : resize.getWidth());
			sb.append(CONCAT_CHAR);
			sb.append(resize.getHeight() <= 0 ? "" : resize.getHeight());
			sb.append(resize.isKeepRatio() ? "" : FORCE_CHAR);
			sb.append(".").append(ImageHelper.JPEG);
		}
		return sb.toString();
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (resizeValidator == null) {
			resizeValidator = new DefaultResizeValidator();
		}
	}

	@Override
	public String getSourcePathByResizePath(String path) {
		return path.substring(0, path.lastIndexOf('/'));
	}

	public void setResizeValidator(ResizeValidator resizeValidator) {
		this.resizeValidator = resizeValidator;
	}

	@Override
	public boolean validResize(Resize resize) {
		return resizeValidator.valid(resize);
	}
}
