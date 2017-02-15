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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import com.madgag.gif.fmsware.GifDecoder;

import me.qyh.blog.exception.SystemException;
import me.qyh.blog.util.FileUtils;
import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.Thumbnails.Builder;

/**
 * 基于java的图片处理，可能会消耗大量的内存和cpu
 * 
 * @author Administrator
 *
 */
public class JavaImageHelper extends ImageHelper {

	@Override
	protected void doResize(Resize resize, File src, File dest) throws IOException {
		String ext = FileUtils.getFileExtension(src.getName());
		File todo = src;
		File tmp = null;
		if (isGIF(ext)) {
			// 获取封面
			tmp = FileUtils.appTemp(PNG);
			doGetGifCover(src, tmp);
			todo = tmp;
		}
		BufferedImage bi = doWithThumbnailator(todo, dest, resize);
		writeImg(bi, FileUtils.getFileExtension(dest.getName()), dest);
	}

	@Override
	protected ImageInfo doRead(File file) throws IOException {
		String ext = FileUtils.getFileExtension(file.getName());
		if (isGIF(ext)) {
			return readGif(file);
		} else {
			return readOtherImage(file);
		}
	}

	private ImageInfo readGif(File file) throws IOException {
		try (InputStream is = new FileInputStream(file)) {
			GifDecoder gd = new GifDecoder();
			int flag = gd.read(is);
			if (flag != GifDecoder.STATUS_OK) {
				throw new IOException(file + "文件无法读取");
			}
			Dimension dim = gd.getFrameSize();
			return new ImageInfo(dim.width, dim.height, GIF);
		}
	}

	private ImageInfo readOtherImage(File file) throws IOException {
		try (InputStream is = new FileInputStream(file)) {
			try (ImageInputStream iis = ImageIO.createImageInputStream(is)) {
				Iterator<ImageReader> imageReaders = ImageIO.getImageReaders(iis);
				while (imageReaders.hasNext()) {
					ImageReader reader = imageReaders.next();
					reader.setInput(iis);
					int minIndex = reader.getMinIndex();
					return new ImageInfo(reader.getWidth(minIndex), reader.getHeight(minIndex), reader.getFormatName());
				}
				throw new IOException("无法确定图片:" + file.getAbsolutePath() + "的具体类型");
			}
		}
	}

	@Override
	protected void doGetGifCover(File gif, File dest) throws IOException {
		File png = null;
		try (InputStream is = new FileInputStream(gif)) {
			GifDecoder gd = new GifDecoder();
			int flag = gd.read(is);
			if (flag != GifDecoder.STATUS_OK) {
				throw new IOException(gif + "文件无法读取");
			}
			BufferedImage bi = gd.getFrame(0);
			png = FileUtils.appTemp(PNG);
			writeImg(bi, PNG, png);
			String destExt = FileUtils.getFileExtension(dest.getName());
			if (isPNG(destExt)) {
				try {
					FileUtils.deleteQuietly(dest);
					FileUtils.move(png, dest);
					return;
				} catch (IOException e) {
					throw new SystemException(e.getMessage(), e);
				}
			}
			// PNG to Other Format
			BufferedImage readed = ImageIO.read(png);
			BufferedImage newBufferedImage = new BufferedImage(readed.getWidth(), readed.getHeight(),
					BufferedImage.TYPE_INT_RGB);
			Graphics2D g2d = newBufferedImage.createGraphics();
			g2d.drawImage(readed, 0, 0, Color.WHITE, null);
			g2d.dispose();
			writeImg(newBufferedImage, destExt, dest);
		}
	}

	@Override
	protected void doFormat(File src, File dest) throws IOException {
		String ext = FileUtils.getFileExtension(src.getName());
		String destExt = FileUtils.getFileExtension(dest.getName());
		if (sameFormat(ext, destExt)) {
			try {
				FileUtils.copy(src, dest);
			} catch (IOException e) {
				throw new SystemException(e.getMessage(), e);
			}
		}
		if (isGIF(ext)) {
			doGetGifCover(src, dest);
		} else {
			BufferedImage readed = ImageIO.read(src);
			BufferedImage newBufferedImage = new BufferedImage(readed.getWidth(), readed.getHeight(),
					BufferedImage.TYPE_INT_RGB);
			newBufferedImage.createGraphics().drawImage(readed, 0, 0, Color.WHITE, null);
			writeImg(newBufferedImage, destExt, dest);
		}
	}

	private void writeImg(BufferedImage bi, String ext, File dest) throws IOException {
		FileUtils.deleteQuietly(dest);
		ImageIO.write(bi, ext, dest);
		bi.flush();
	}

	private BufferedImage doWithThumbnailator(File todo, File dest, Resize resize) throws IOException {
		BufferedImage originalImage = ImageIO.read(todo);
		int width = originalImage.getWidth();
		int height = originalImage.getHeight();
		int resizeWidth;
		int resizeHeight;
		if (resize.getSize() != null) {
			int size = resize.getSize();
			if (width > height) {
				resizeWidth = size > width ? width : size;
				resizeHeight = resizeWidth * height / width;
			} else if (width < height) {
				resizeHeight = size > height ? height : size;
				resizeWidth = resizeHeight * width / height;
			} else {
				resizeWidth = resizeHeight = size > width ? width : size;
			}
		} else {
			if (resize.isKeepRatio()) {
				return doWithThumbnailator(todo, dest, new Resize(Math.max(resize.getWidth(), resize.getHeight())));
			} else {
				resizeWidth = (resize.getWidth() > width) ? width : resize.getWidth();
				resizeHeight = (resize.getHeight() > height) ? height : resize.getHeight();
			}
		}
		String destExt = FileUtils.getFileExtension(dest.getName());
		Builder<BufferedImage> builder = Thumbnails.of(originalImage);
		if (!maybeTransparentBg(destExt)) {
			// 防止红色背景
			builder = builder.imageType(BufferedImage.TYPE_INT_RGB);
		}
		return builder.size(resizeWidth, resizeHeight).asBufferedImage();
	}

	@Override
	public final boolean supportWebp() {
		return false;
	}
}
