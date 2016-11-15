package me.qyh.blog.file;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;

import com.madgag.gif.fmsware.GifDecoder;

import me.qyh.blog.exception.SystemException;
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
		String ext = FilenameUtils.getExtension(src.getName());
		File todo = src;
		File tmp = null;
		try {
			if (isGIF(ext)) {
				// 获取封面
				tmp = FileHelper.temp(PNG);
				doGetGifCover(src, tmp);
				todo = tmp;
			}
			BufferedImage bi = doWithThumbnailator(todo, dest, resize);
			writeImg(bi, FilenameUtils.getExtension(dest.getName()), dest);
		} finally {
			FileUtils.deleteQuietly(tmp);
		}
	}

	@Override
	protected ImageInfo doRead(File file) throws IOException {
		String ext = FilenameUtils.getExtension(file.getName());
		if (isGIF(ext)) {
			return readGif(file);
		} else {
			return readOtherImage(file);
		}
	}

	private ImageInfo readGif(File file) throws IOException {
		InputStream is = null;
		try {
			is = FileHelper.openStream(file);
			GifDecoder gd = new GifDecoder();
			int flag = gd.read(is);
			if (flag != GifDecoder.STATUS_OK)
				throw new IOException(file + "文件无法读取");
			Dimension dim = gd.getFrameSize();
			return new ImageInfo(dim.width, dim.height, GIF);
		} finally {
			IOUtils.closeQuietly(is);
		}
	}

	private ImageInfo readOtherImage(File file) throws IOException {
		InputStream is = null;
		try {
			is = FileHelper.openStream(file);
			ImageInputStream iis = null;
			try {
				iis = ImageIO.createImageInputStream(is);
				Iterator<ImageReader> imageReaders = ImageIO.getImageReaders(iis);
				while (imageReaders.hasNext()) {
					ImageReader reader = imageReaders.next();
					reader.setInput(iis);
					int minIndex = reader.getMinIndex();
					return new ImageInfo(reader.getWidth(reader.getMinIndex()), reader.getHeight(minIndex),
							reader.getFormatName());
				}
				throw new IOException("无法确定图片:" + file.getAbsolutePath() + "的具体类型");
			} finally {
				IOUtils.closeQuietly(iis);
			}
		} finally {
			IOUtils.closeQuietly(is);
		}
	}

	@Override
	protected void doGetGifCover(File gif, File dest) throws IOException {
		InputStream is = null;
		File png = null;
		try {
			is = FileHelper.openStream(gif);
			GifDecoder gd = new GifDecoder();
			int flag = gd.read(is);
			if (flag != GifDecoder.STATUS_OK)
				throw new IOException(gif + "文件无法读取");
			BufferedImage bi = gd.getFrame(0);
			png = FileHelper.temp(PNG);
			writeImg(bi, PNG, png);
			String destExt = FilenameUtils.getExtension(dest.getName());
			if (isPNG(destExt))
				try {
					FileUtils.deleteQuietly(dest);
					FileUtils.moveFile(png, dest);
					return;
				} catch (IOException e) {
					throw new SystemException(e.getMessage(), e);
				}
			// PNG to Other Format
			BufferedImage readed = ImageIO.read(png);
			BufferedImage newBufferedImage = new BufferedImage(readed.getWidth(), readed.getHeight(),
					BufferedImage.TYPE_INT_RGB);
			Graphics2D g2d = newBufferedImage.createGraphics();
			g2d.drawImage(readed, 0, 0, Color.WHITE, null);
			g2d.dispose();
			writeImg(newBufferedImage, destExt, dest);
		} finally {
			FileUtils.deleteQuietly(png);
			IOUtils.closeQuietly(is);
		}
	}

	@Override
	protected void doFormat(File src, File dest) throws IOException {
		String ext = FilenameUtils.getExtension(src.getName());
		String destExt = FilenameUtils.getExtension(dest.getName());
		if (sameFormat(ext, destExt))
			try {
				FileUtils.copyFile(src, dest);
			} catch (IOException e) {
				throw new SystemException(e.getMessage(), e);
			}
		if (isGIF(ext))
			doGetGifCover(src, dest);
		else {
			BufferedImage readed = ImageIO.read(src);
			BufferedImage newBufferedImage = new BufferedImage(readed.getWidth(), readed.getHeight(),
					BufferedImage.TYPE_INT_RGB);
			newBufferedImage.createGraphics().drawImage(readed, 0, 0, Color.WHITE, null);
			writeImg(newBufferedImage, destExt, dest);
		}
	}

	@Override
	public boolean supportFormat(String extension) {
		return isGIF(extension) || isJPEG(extension) || isPNG(extension);
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
		String destExt = FilenameUtils.getExtension(dest.getName());
		Builder<BufferedImage> builder = Thumbnails.of(originalImage);
		if (!maybeTransparentBg(destExt))
			// 防止红色背景
			builder = builder.imageType(BufferedImage.TYPE_INT_RGB);
		return builder.size(resizeWidth, resizeHeight).asBufferedImage();
	}
}
