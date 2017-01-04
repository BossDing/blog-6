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

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.imageio.ImageIO;

import org.im4java.core.ConvertCmd;
import org.im4java.core.IM4JavaException;
import org.im4java.core.IMOperation;
import org.im4java.core.IdentifyCmd;
import org.im4java.process.ArrayListOutputConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import com.google.common.io.Files;
import com.madgag.gif.fmsware.GifDecoder;

import me.qyh.blog.exception.SystemException;
import me.qyh.blog.util.FileUtils;
import me.qyh.blog.util.Validators;

/**
 * 图片处理类，基于{@link http://www.graphicsmagick.org/}，可以用来处理PNG,JPEG,GIF,WEBP等多种格式
 * 
 * @author Administrator
 *
 */
public class GraphicsMagickImageHelper extends ImageHelper implements InitializingBean {

	private static final Logger LOGGER = LoggerFactory.getLogger(GraphicsMagickImageHelper.class);

	/**
	 * 在windows环境下，必须设置这个路径
	 */
	private String magickPath;

	private static final boolean WINDOWS = File.separatorChar == '\\';

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
	protected void doResize(Resize resize, File src, File dest) throws IOException {
		IMOperation op = new IMOperation();
		op.addImage();
		setResize(resize, op);
		String ext = Files.getFileExtension(dest.getName());
		String srcExt = Files.getFileExtension(src.getName());
		if (!maybeTransparentBg(ext) || !maybeTransparentBg(srcExt)) {
			setWhiteBg(op);
		}
		op.strip();
		op.p_profile("*");
		if (interlace(dest)) {
			op.interlace("Line");
		}
		op.addImage();
		try {
			getConvertCmd().run(op, src.getAbsolutePath() + "[0]", dest.getAbsolutePath());
		} catch (IOException | InterruptedException e) {
			throw new SystemException(e.getMessage(), e);
		} catch (IM4JavaException e) {
			throw new IOException(e.getMessage(), e);
		}
	}

	protected void setResize(Resize resize, IMOperation op) {
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
	}

	@Override
	protected ImageInfo doRead(File file) throws IOException {
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
			throw new IOException(e.getMessage(), e);
		}
	}

	@Override
	protected void doGetGifCover(File gif, File dest) throws IOException {
		String ext = Files.getFileExtension(dest.getName());
		File _gif = FileUtils.temp(GIF);
		try {
			IMOperation op = new IMOperation();
			op.addImage();
			op.strip();
			op.p_profile("*");
			op.addImage();
			getConvertCmd().run(op, gif.getAbsolutePath() + "[0]", _gif.getAbsolutePath());
		} catch (Exception e) {
			LOGGER.debug("GraphicsMagick无法获取" + gif.getAbsolutePath() + "这张图片的封面，尝试用GifDecoder来获取", e);
			getGifCoverUseJava(gif, _gif);
		}
		if (Files.getFileExtension(dest.getName()).equalsIgnoreCase(GIF)) {
			// copy
			Files.copy(_gif, dest);
			return;
		}
		// png to dest
		IMOperation op = new IMOperation();
		op.addImage();
		if (!maybeTransparentBg(ext)) {
			setWhiteBg(op);
		}
		op.strip();
		op.p_profile("*");
		op.addImage();
		try {
			getConvertCmd().run(op, _gif.getAbsolutePath(), dest.getAbsolutePath());
		} catch (Exception e1) {
			throw new SystemException(e1.getMessage(), e1);
		} finally {
			if (_gif != null && _gif.exists()) {
				FileUtils.deleteQuietly(_gif);
			}
		}
	}

	@Override
	protected void doFormat(File src, File dest) throws IOException {
		IMOperation op = new IMOperation();
		op.addImage();
		String ext = Files.getFileExtension(dest.getName());
		String srcExt = Files.getFileExtension(src.getName());
		if (!maybeTransparentBg(ext) || !maybeTransparentBg(srcExt)) {
			setWhiteBg(op);
		}
		op.strip();
		op.p_profile("*");
		op.addImage();
		try {
			getConvertCmd().run(op, src.getAbsolutePath() + "[0]", dest.getAbsolutePath());
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new SystemException(e.getMessage(), e);
		} catch (IM4JavaException e) {
			throw new IOException(e.getMessage(), e);
		}
	}

	@Override
	public boolean supportFormat(String extension) {
		return Arrays.stream(IMG_EXTENSIONS).anyMatch(ext -> ext.equalsIgnoreCase(extension));
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (WINDOWS && Validators.isEmptyOrNull(magickPath, true)) {
			throw new SystemException("windows下必须设置GraphicsMagick的主目录");
		}
	}

	private boolean interlace(File dest) {
		if (!doInterlace) {
			return false;
		}
		String ext = Files.getFileExtension(dest.getName());
		return isGIF(ext) || isPNG(ext) || isJPEG(ext);
	}

	private void getGifCoverUseJava(File gif, File _gif) throws IOException {
		GifDecoder gd = new GifDecoder();
		try (InputStream is = new FileInputStream(gif)) {
			int flag = gd.read(is);
			if (flag != GifDecoder.STATUS_OK) {
				throw new IOException(gif + "文件无法获取封面");
			}
			BufferedImage bi = gd.getFrame(0);
			ImageIO.write(bi, GIF, _gif);
		}
	}

	private void setWhiteBg(IMOperation op) {
		op.background("rgb(255,255,255)");
		op.extent(0, 0);
		op.addRawArgs("+matte");
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

	public void setMagickPath(String magickPath) {
		this.magickPath = magickPath;
	}
}
