//package me.qyh.blog.file;
//
//import java.awt.Color;
//import java.awt.Dimension;
//import java.awt.Graphics2D;
//import java.awt.image.BufferedImage;
//import java.io.File;
//import java.io.FileInputStream;
//import java.io.FileNotFoundException;
//import java.io.IOException;
//import java.io.InputStream;
//import java.nio.file.Files;
//import java.util.Iterator;
//
//import javax.imageio.ImageIO;
//import javax.imageio.ImageReader;
//import javax.imageio.stream.ImageInputStream;
//
//import org.apache.commons.io.FileUtils;
//import org.apache.commons.io.FilenameUtils;
//import org.apache.commons.io.IOUtils;
//import org.apache.commons.lang3.RandomStringUtils;
//
//import com.madgag.gif.fmsware.GifDecoder;
//
//import me.qyh.blog.exception.SystemException;
//import net.coobird.thumbnailator.Thumbnails;
//import net.coobird.thumbnailator.Thumbnails.Builder;
//
//public class JavaImageHelper extends ImageHelper {
//
//	/**
//	 * 如果是GIF缩放，由于本身的支持，这里只输出基于封面(第一帧)的缩放
//	 */
//	@Override
//	protected void _resize(Resize resize, File src, File dest) throws ImageReadWriteException {
//		String ext = FilenameUtils.getExtension(src.getName());
//		File todo = src;
//		if (isGIF(ext)) {
//			// 获取封面
//			File tmp = null;
//			try {
//				tmp = createTmp(".png");
//			} finally {
//				FileUtils.deleteQuietly(tmp);
//			}
//			_getGifCover(src, tmp);
//			todo = tmp;
//		}
//		BufferedImage bi = doWithThumbnailator(todo, dest, resize);
//		writeImg(bi, FilenameUtils.getExtension(dest.getName()), dest);
//	}
//
//	@Override
//	protected ImageInfo _read(File file) throws ImageReadWriteException {
//		String ext = FilenameUtils.getExtension(file.getName());
//		InputStream is = null;
//		try {
//			try {
//				is = new FileInputStream(file);
//			} catch (FileNotFoundException e) {
//				throw new SystemException(e.getMessage(), e);
//			}
//
//			if (isGIF(ext)) {
//				GifDecoder gd = new GifDecoder();
//				int flag = gd.read(is);
//				if (flag != GifDecoder.STATUS_OK)
//					throw new ImageReadWriteException(file + "文件无法读取");
//				Dimension dim = gd.getFrameSize();
//				return new ImageInfo(dim.width, dim.height, GIF);
//			} else {
//				try {
//					ImageInputStream iis = ImageIO.createImageInputStream(is);
//					try {
//						Iterator<ImageReader> imageReaders = ImageIO.getImageReaders(iis);
//						while (imageReaders.hasNext()) {
//							ImageReader reader = (ImageReader) imageReaders.next();
//							reader.setInput(iis);
//							int minIndex = reader.getMinIndex();
//							return new ImageInfo(reader.getWidth(reader.getMinIndex()), reader.getHeight(minIndex),
//									reader.getFormatName());
//						}
//						throw new ImageReadWriteException("无法确定图片的具体类型");
//					} finally {
//						IOUtils.closeQuietly(iis);
//					}
//				} catch (IOException e) {
//					throw new ImageReadWriteException(e.getMessage(), e);
//				}
//			}
//		} finally {
//			IOUtils.closeQuietly(is);
//		}
//	}
//
//	@Override
//	protected void _getGifCover(File gif, File dest) throws ImageReadWriteException {
//		InputStream is = null;
//		File png = null;
//		try {
//			try {
//				is = new FileInputStream(gif);
//			} catch (FileNotFoundException e) {
//				throw new SystemException(e.getMessage(), e);
//			}
//			GifDecoder gd = new GifDecoder();
//			int flag = gd.read(is);
//			if (flag != GifDecoder.STATUS_OK)
//				throw new ImageReadWriteException(gif + "文件无法读取");
//			BufferedImage bi = gd.getFrame(0);
//			png = createTmp(".png");
//			writeImg(bi, PNG, png);
//			String destExt = FilenameUtils.getExtension(dest.getName());
//			if (isPNG(destExt))
//				try {
//					FileUtils.deleteQuietly(dest);
//					FileUtils.moveFile(png, dest);
//					return;
//				} catch (IOException e) {
//					throw new SystemException(e.getMessage(), e);
//				}
//			// PNG to Other Format
//			BufferedImage _bi = readImg(png);
//			BufferedImage newBufferedImage = new BufferedImage(_bi.getWidth(), _bi.getHeight(),
//					BufferedImage.TYPE_INT_RGB);
//			Graphics2D g2d = newBufferedImage.createGraphics();
//			g2d.drawImage(_bi, 0, 0, Color.WHITE, null);
//			g2d.dispose();
//			writeImg(newBufferedImage, destExt, dest);
//		} finally {
//			FileUtils.deleteQuietly(png);
//			IOUtils.closeQuietly(is);
//		}
//	}
//
//	@Override
//	protected void _format(File src, File dest) throws ImageReadWriteException {
//		String ext = FilenameUtils.getExtension(src.getName());
//		String destExt = FilenameUtils.getExtension(dest.getName());
//		if (sameFormat(ext, destExt))
//			try {
//				FileUtils.copyFile(src, dest);
//			} catch (IOException e) {
//				throw new SystemException(e.getMessage(), e);
//			}
//		if (isGIF(ext))
//			_getGifCover(src, dest);
//		else {
//			BufferedImage _bi = readImg(src);
//			BufferedImage newBufferedImage = new BufferedImage(_bi.getWidth(), _bi.getHeight(),
//					BufferedImage.TYPE_INT_RGB);
//			newBufferedImage.createGraphics().drawImage(_bi, 0, 0, Color.WHITE, null);
//			writeImg(newBufferedImage, destExt, dest);
//		}
//	}
//
//	@Override
//	public boolean supportFormat(String extension) {
//		return isGIF(extension) || isJPEG(extension) || isPNG(extension);
//	}
//
//	private void writeImg(BufferedImage bi, String ext, File dest) throws ImageReadWriteException {
//		FileUtils.deleteQuietly(dest);
//		try {
//			ImageIO.write(bi, ext, dest);
//			bi.flush();
//		} catch (IOException e) {
//			throw new ImageReadWriteException(e.getMessage(), e);
//		}
//	}
//
//	private BufferedImage readImg(File file) throws ImageReadWriteException {
//		try {
//			return ImageIO.read(file);
//		} catch (IOException e) {
//			throw new ImageReadWriteException(e.getMessage(), e);
//		}
//	}
//
//	private File createTmp(String suffix) {
//		try {
//			return Files.createTempFile(RandomStringUtils.randomNumeric(6), suffix).toFile();
//		} catch (IOException e) {
//			throw new SystemException(e.getMessage(), e);
//		}
//	}
//
//	private BufferedImage doWithThumbnailator(File todo, File dest, Resize resize) throws ImageReadWriteException {
//		BufferedImage originalImage = readImg(todo);
//		int width = originalImage.getWidth();
//		int height = originalImage.getHeight();
//		int resizeWidth = 0;
//		int resizeHeight = 0;
//		if (resize.getSize() != null) {
//			int size = resize.getSize();
//			if (width > height) {
//				resizeWidth = size > width ? width : size;
//				resizeHeight = resizeWidth * height / width;
//			} else if (width < height) {
//				resizeHeight = size > height ? height : size;
//				resizeWidth = resizeHeight * width / height;
//			} else {
//				resizeWidth = resizeHeight = (size > width ? width : size);
//			}
//		} else {
//			if (resize.isKeepRatio()) {
//				return doWithThumbnailator(todo, dest, new Resize(Math.max(resize.getWidth(), resize.getHeight())));
//			} else {
//				resizeWidth = (resize.getWidth() > width) ? width : resize.getWidth();
//				resizeHeight = (resize.getHeight() > height) ? height : resize.getHeight();
//			}
//		}
//		try {
//			String destExt = FilenameUtils.getExtension(dest.getName());
//			Builder<BufferedImage> builder = Thumbnails.of(originalImage);
//			if (!maybeTransparentBg(destExt))
//				// 防止红色背景
//				builder = builder.imageType(BufferedImage.TYPE_INT_RGB);
//			return builder.size(resizeWidth, resizeHeight).asBufferedImage();
//		} catch (IOException e) {
//			throw new ImageReadWriteException(e.getMessage(), e);
//		}
//	}
//}
