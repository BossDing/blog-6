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

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.function.Function;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import com.madgag.gif.fmsware.GifDecoder;

import me.qyh.blog.exception.SystemException;
import me.qyh.blog.util.FileUtils;

/**
 * 基于java的图片处理，可能会消耗大量的内存和cpu
 * <p>
 * <b>这个类仅供测试使用，实际请使用<code>GraphicsMagickImageHelper</code></b>
 * </p>
 * 
 * @see GraphicsMagickImageHelper
 * @author Administrator
 *
 */
public class JavaImageHelper extends ImageHelper {

	private static final WhiteBgFilter WHITE_BG_FILTER = new WhiteBgFilter();

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
		BufferedImage bi = doResize(todo, dest, resize);
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

	private void doGetGifCover(File gif, File dest) throws IOException {
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
			writeImg(WHITE_BG_FILTER.apply(readed), destExt, dest);
		}
	}

	@Override
	protected void doFormat(File src, File dest) throws IOException {
		String ext = FileUtils.getFileExtension(src.getName());
		String destExt = FileUtils.getFileExtension(dest.getName());
		if (isGIF(ext)) {
			doGetGifCover(src, dest);
		} else {
			BufferedImage readed = ImageIO.read(src);
			writeImg(WHITE_BG_FILTER.apply(readed), destExt, dest);
		}
	}

	private void writeImg(BufferedImage bi, String ext, File dest) throws IOException {
		FileUtils.deleteQuietly(dest);
		ImageIO.write(bi, ext, dest);
		bi.flush();
	}

	private BufferedImage doResize(File todo, File dest, Resize resize) throws IOException {
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
				return doResize(todo, dest, new Resize(Math.max(resize.getWidth(), resize.getHeight())));
			} else {
				resizeWidth = (resize.getWidth() > width) ? width : resize.getWidth();
				resizeHeight = (resize.getHeight() > height) ? height : resize.getHeight();
			}
		}
		String destExt = FileUtils.getFileExtension(dest.getName());

		boolean maybeTransparentBy = maybeTransparentBg(destExt);
		int imageType = maybeTransparentBy ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
		BufferedImage scaledBI = new BufferedImage(resizeWidth, resizeHeight, imageType);
		Graphics2D g = scaledBI.createGraphics();
		if (!maybeTransparentBy) {
			g.setComposite(AlphaComposite.Src);
			g.drawImage(originalImage, 0, 0, resizeWidth, resizeHeight, Color.WHITE, null);
		} else {
			g.drawImage(originalImage, 0, 0, resizeWidth, resizeHeight, null);
		}
		g.dispose();

		return scaledBI;
	}

	private static final class WhiteBgFilter implements Function<BufferedImage, BufferedImage> {

		@Override
		public BufferedImage apply(BufferedImage img) {

			int width = img.getWidth();
			int height = img.getHeight();

			BufferedImage finalImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
			Graphics2D g = finalImage.createGraphics();
			g.drawImage(img, 0, 0, Color.WHITE, null);
			g.dispose();

			return finalImage;
		}
	}

	@Override
	public final boolean supportWebp() {
		return false;
	}
}
