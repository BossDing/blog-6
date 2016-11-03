package me.qyh.blog.file;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.im4java.core.ConvertCmd;
import org.im4java.core.IMOperation;
import org.im4java.core.IdentifyCmd;
import org.im4java.process.ArrayListOutputConsumer;
import org.springframework.beans.factory.InitializingBean;

import com.madgag.gif.fmsware.GifDecoder;

import me.qyh.blog.exception.SystemException;
import me.qyh.blog.file.local.ImageReadException;
import me.qyh.util.Validators;

public class GraphicsMagickImageHelper extends ImageHelper implements InitializingBean {

	/**
	 * 在windows环境下，必须设置这个路径
	 */
	private String magickPath;

	private static final boolean WINDOWS = (File.separatorChar == '\\');

	/**
	 * 如果为true，那么将会以渐进的方式显示出来，但在有些浏览器，例如EDGE则会先显示空白后再显示图片
	 */
	private boolean doInterlace = true;

	/**
	 * <p>
	 * 用来指定缩放后的文件信息,如果指定了纵横比但同时指定了缩略图宽度和高度，将会以宽度或者长度为准(具体图片不同),如果只希望将长度(或宽度进行缩放)
	 * ， 那么只要将另一个长度置位 <=0就可以了 如果不保持纵横比同时没有指定宽度和高度(都<=0)将返回原图链接<br/>
	 * <strong>缩略图将只会返回jpg格式的图片 </strong><br/>
	 * <strong>默认情况下总是缩放(即比原图小)</strong>
	 * </p>
	 * 
	 * @author Administrator
	 *
	 */
	@Override
	public void resize(Resize resize, File src, File dest) throws Exception {
		IMOperation op = new IMOperation();
		op.addImage();
		if (resize.getSize() != null) {
			op.resize(resize.getSize(), resize.getSize(), '>');
		} else {
			if (!resize.isKeepRatio()) {
				op.resize(resize.getWidth(), resize.getHeight(), '!');
			} else {
				if (resize.getWidth() <= 0) {
					op.resize(Integer.MAX_VALUE, resize.getHeight(), '>');
				} else if (resize.getHeight() <= 0) {
					op.resize(resize.getWidth(), Integer.MAX_VALUE, '>');
				} else {
					op.resize(resize.getWidth(), resize.getHeight(), '>');
				}
			}
		}
		String ext = FilenameUtils.getExtension(dest.getName());
		String srcExt = FilenameUtils.getExtension(src.getName());
		if (!maybeTransparentBg(ext) || !maybeTransparentBg(srcExt)) {
			op.background("rgb(255,255,255)");
			op.extent(0, 0);
			op.addRawArgs("+matte");
		}
		op.strip();
		op.p_profile("*");
		if (interlace(dest))
			op.interlace("Line");
		op.addImage();
		getConvertCmd().run(op, src.getAbsolutePath() + "[0]", dest.getAbsolutePath());
	}

	private boolean interlace(File dest) {
		if (!doInterlace)
			return false;
		String ext = FilenameUtils.getExtension(dest.getName());
		return isGIF(ext) || isPNG(ext) || isJPEG(ext);
	}

	@Override
	public ImageInfo read(File file) throws ImageReadException {
		IMOperation localIMOperation = new IMOperation();
		localIMOperation.ping();
		localIMOperation.format("%w\n%h\n%m\n");
		localIMOperation.addImage();
		IdentifyCmd localIdentifyCmd = new IdentifyCmd(true);
		if (WINDOWS) {
			localIdentifyCmd.setSearchPath(magickPath);
		}
		ArrayListOutputConsumer localArrayListOutputConsumer = new ArrayListOutputConsumer();
		localIdentifyCmd.setOutputConsumer(localArrayListOutputConsumer);
		try {
			localIdentifyCmd.run(localIMOperation, file.getAbsolutePath() + "[0]");
			List<String> atts = localArrayListOutputConsumer.getOutput();
			Iterator<String> it = atts.iterator();
			return new ImageInfo(Integer.parseInt(it.next()), Integer.parseInt(it.next()), it.next());
		} catch (Exception e) {
			throw new ImageReadException(e.getMessage(), e);
		}
	}

	public void setMagickPath(String magickPath) {
		this.magickPath = magickPath;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (WINDOWS && Validators.isEmptyOrNull(magickPath, true)) {
			throw new SystemException("windows下必须设置GraphicsMagick的主目录");
		}
	}

	@Override
	public void getGifCover(File gif, File dest) throws ImageReadException {
		String ext = FilenameUtils.getExtension(dest.getName());
		File png = null;
		try {
			png = File.createTempFile(RandomStringUtils.random(6), "." + PNG);
		} catch (IOException e) {
			throw new SystemException(e.getMessage(), e);
		}
		try {
			IMOperation op = new IMOperation();
			op.addImage();
			op.strip();
			op.p_profile("*");
			op.addImage();
			getConvertCmd().run(op, gif.getAbsolutePath() + "[0]", png.getAbsolutePath());
		} catch (Exception e) {
			// Corrupt Image
			GifDecoder gd = new GifDecoder();
			InputStream is = null;
			try {
				try {
					is = new FileInputStream(gif);
				} catch (FileNotFoundException e1) {
					throw new SystemException(e1.getMessage(), e1);
				}
				int flag = gd.read(is);
				if (flag != GifDecoder.STATUS_OK) {
					throw new ImageReadException(gif + "文件无法获取封面");
				}
				BufferedImage bi = gd.getFrame(0);
				try {
					png = Files.createTempFile("tmp", "." + PNG).toFile();
					ImageIO.write(bi, PNG, png);
				} catch (IOException e1) {
					throw new SystemException(e1.getMessage(), e1);
				}
			} finally {
				IOUtils.closeQuietly(is);
			}
		}
		// png to dest
		IMOperation op = new IMOperation();
		op.addImage();
		if (!maybeTransparentBg(ext)) {
			op.background("rgb(255,255,255)");
			op.extent(0, 0);
			op.addRawArgs("+matte");
		}
		op.strip();
		op.p_profile("*");
		op.addImage();
		try {
			getConvertCmd().run(op, png.getAbsolutePath(), dest.getAbsolutePath());
		} catch (Exception e1) {
			throw new SystemException(e1.getMessage(), e1);
		} finally {
			if (png != null && png.exists() && !FileUtils.deleteQuietly(png))
				png.deleteOnExit();
		}
	}

	@Override
	public void format(File src, File dest) throws Exception {
		IMOperation op = new IMOperation();
		op.addImage();
		String ext = FilenameUtils.getExtension(dest.getName());
		String srcExt = FilenameUtils.getExtension(src.getName());
		if (!maybeTransparentBg(ext) || !maybeTransparentBg(srcExt)) {
			op.background("rgb(255,255,255)");
			op.extent(0, 0);
			op.addRawArgs("+matte");
		}
		op.strip();
		op.p_profile("*");
		op.addImage();
		getConvertCmd().run(op, src.getAbsolutePath() + "[0]", dest.getAbsolutePath());
	}

	private ConvertCmd getConvertCmd() {
		ConvertCmd cmd = new ConvertCmd(true);
		if (WINDOWS) {
			cmd.setSearchPath(magickPath);
		}
		return cmd;
	}

	public void setDoInterlace(boolean doInterlace) {
		this.doInterlace = doInterlace;
	}
}
