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
import java.io.IOException;

import me.qyh.blog.util.FileUtils;

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

	protected static final String[] IMG_EXTENSIONS = { GIF, JPEG, JPG, PNG, WEBP };

	/**
	 * 缩放图片
	 * 
	 * @param resize
	 *            缩放尺寸
	 * @param src
	 *            原文件
	 * @param dest
	 *            目标文件
	 * @throws IOException
	 *             文件读写失败
	 */
	public final void resize(Resize resize, File src, File dest) throws IOException {
		formatCheck(src);
		formatCheck(dest);
		doResize(resize, src, dest);
	}

	/**
	 * 读取图片信息
	 * 
	 * @param file
	 *            图片文件
	 * @return
	 * @throws IOException
	 *             文件读取失败
	 */
	public final ImageInfo read(File file) throws IOException {
		formatCheck(file);
		ImageInfo ii = doRead(file);
		if (!supportFormat(ii.getExtension())) {
			throw new IOException("文件格式" + ii.extension + "不被支持");
		}
		return ii;

	}

	/**
	 * 获取gif文件的封面
	 * 
	 * @param gif
	 *            gif文件
	 * @param dest
	 *            封面文件
	 * @throws IOException
	 *             读写异常
	 */
	public final void getGifCover(File gif, File dest) throws IOException {
		formatCheck(gif);
		formatCheck(dest);
		doGetGifCover(gif, dest);
	}

	/**
	 * 图片格式转化
	 * 
	 * @param src
	 *            原文件
	 * @param dest
	 *            目标文件
	 * @throws IOException
	 *             读写异常
	 */
	public final void format(File src, File dest) throws IOException {
		formatCheck(src);
		formatCheck(dest);
		doFormat(src, dest);
	}

	/**
	 * 是否支持格式
	 * 
	 * @param extension
	 * @return
	 */
	public abstract boolean supportFormat(String extension);

	protected abstract void doResize(Resize resize, File src, File dest) throws IOException;

	protected abstract ImageInfo doRead(File file) throws IOException;

	protected abstract void doGetGifCover(File gif, File dest) throws IOException;

	protected abstract void doFormat(File src, File dest) throws IOException;

	/**
	 * 图片信息
	 * 
	 * @author Administrator
	 *
	 */
	public final class ImageInfo {
		private final int width;
		private final int height;
		private final String extension;// 图片真实后缀

		/**
		 * 
		 * @param width
		 *            宽
		 * @param height
		 *            高
		 * @param extension
		 *            实际后缀
		 */
		protected ImageInfo(int width, int height, String extension) {
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

	/**
	 * 判断图片是否为jpeg格式的图片
	 * 
	 * @param extension
	 *            图片后缀
	 * @return 是: true ,否:false
	 */
	public static boolean isJPEG(String extension) {
		return JPEG.equalsIgnoreCase(extension) || JPG.equalsIgnoreCase(extension);
	}

	/**
	 * 判断图片是否为webp格式的图片
	 * 
	 * @param extension
	 *            图片后缀
	 * @return 是: true ,否:false
	 */
	public static boolean isWEBP(String extension) {
		return WEBP.equalsIgnoreCase(extension);
	}

	/**
	 * 判断图片是否为png格式的图片
	 * 
	 * @param extension
	 *            图片后缀
	 * @return 是: true ,否:false
	 */
	public static boolean isPNG(String extension) {
		return PNG.equalsIgnoreCase(extension);
	}

	/**
	 * 判断图片是否为gif格式的图片
	 * 
	 * @param extension
	 *            图片后缀
	 * @return 是: true ,否:false
	 */
	public static boolean isGIF(String extension) {
		return GIF.equalsIgnoreCase(extension);
	}

	/**
	 * 判断后缀是否指向相同的图片格式
	 * 
	 * @param ext1
	 *            后缀1
	 * @param ext2
	 *            后缀2
	 * @return 是: true ,否:false
	 */
	public static boolean sameFormat(String ext1, String ext2) {
		if (ext1.equalsIgnoreCase(ext2)) {
			return true;
		}
		return isJPEG(ext1) && isJPEG(ext2);
	}

	/**
	 * 判断图片可能是允许透明的
	 * 
	 * @param extension
	 *            图片后缀
	 * @return 是:true,否:false
	 */
	public static boolean maybeTransparentBg(String extension) {
		return isPNG(extension) || isGIF(extension) || isWEBP(extension);
	}

	private void formatCheck(File file) throws IOException {
		String extension = FileUtils.getFileExtension(file.getName());
		if (!supportFormat(extension)) {
			throw new IOException("文件格式" + extension + "不被支持");
		}
	}
}
