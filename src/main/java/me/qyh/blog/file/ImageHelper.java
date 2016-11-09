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
package me.qyh.blog.file;

import java.io.File;

import org.apache.commons.io.FilenameUtils;

/**
 * 图片辅助类，用来处理图片格式的转化，缩放以及图片的读取
 * 
 * @author Administrator
 *
 */
public abstract class ImageHelper {

	public static final String GIF = "gif";
	public static final String JPEG = "jpeg";
	public static final String JPG = "jpg";
	public static final String PNG = "png";
	public static final String WEBP = "webp";

	public static final String[] IMG_EXTENSIONS = { GIF, JPEG, JPG, PNG, WEBP };

	public final void resize(Resize resize, File src, File dest)
			throws UnsupportFormatException, ImageReadWriteException {
		formatCheck(src);
		formatCheck(dest);
		_resize(resize, src, dest);
	}

	public final ImageInfo read(File file) throws UnsupportFormatException, ImageReadWriteException {
		formatCheck(file);
		ImageInfo ii = _read(file);
		if (!supportFormat(ii.getExtension()))
			throw new UnsupportFormatException(ii.getExtension());
		return ii;

	}

	public final void getGifCover(File gif, File dest) throws UnsupportFormatException, ImageReadWriteException {
		formatCheck(gif);
		formatCheck(dest);
		_getGifCover(gif, dest);
	}

	public final void format(File src, File dest) throws UnsupportFormatException, ImageReadWriteException {
		formatCheck(src);
		formatCheck(dest);
		_format(src, dest);
	}

	protected abstract void _resize(Resize resize, File src, File dest) throws ImageReadWriteException;

	protected abstract ImageInfo _read(File file) throws ImageReadWriteException;

	protected abstract void _getGifCover(File gif, File dest) throws ImageReadWriteException;

	protected abstract void _format(File src, File dest) throws ImageReadWriteException;

	public abstract boolean supportFormat(String extension);

	public final class ImageInfo {
		private int width;
		private int height;
		private String extension;// 图片真实后缀

		public ImageInfo(int width, int height, String extension) {
			super();
			this.width = width;
			this.height = height;
			this.extension = extension;
		}

		public int getWidth() {
			return width;
		}

		public int getHeight() {
			return height;
		}

		public String getExtension() {
			return extension;
		}

		@Override
		public String toString() {
			return "ImageInfo [width=" + width + ", height=" + height + ", extension=" + extension + "]";
		}
	}

	public static boolean isJPEG(String extension) {
		return JPEG.equalsIgnoreCase(extension) || JPG.equalsIgnoreCase(extension);
	}

	public static boolean isWEBP(String extension) {
		return WEBP.equalsIgnoreCase(extension);
	}

	public static boolean isPNG(String extension) {
		return PNG.equalsIgnoreCase(extension);
	}

	public static boolean isGIF(String extension) {
		return GIF.equalsIgnoreCase(extension);
	}

	public static boolean sameFormat(String ext1, String ext2) {
		if (ext1.equalsIgnoreCase(ext2))
			return true;
		return isJPEG(ext1) && isJPEG(ext2);
	}

	public static boolean maybeTransparentBg(String extension) {
		return isPNG(extension) || isGIF(extension) || isWEBP(extension);
	}

	private void formatCheck(File file) throws UnsupportFormatException {
		String extension = FilenameUtils.getExtension(file.getName());
		if (!supportFormat(extension))
			throw new UnsupportFormatException(extension);
	}
}
