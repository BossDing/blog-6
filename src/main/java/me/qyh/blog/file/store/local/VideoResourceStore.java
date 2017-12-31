package me.qyh.blog.file.store.local;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;

import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import me.qyh.blog.core.exception.LogicException;
import me.qyh.blog.core.util.FileUtils;
import me.qyh.blog.file.entity.CommonFile;
import me.qyh.blog.file.store.ProcessUtils;

public class VideoResourceStore extends ThumbnailSupport {

	private final String[] allowExtensions;

	private final int timeoutSecond;

	public VideoResourceStore(String urlPatternPrefix, String[] allowExtensions, int timeoutSecond) {
		super(urlPatternPrefix);
		this.allowExtensions = allowExtensions;
		this.timeoutSecond = timeoutSecond;
	}

	public VideoResourceStore() {
		this("video", new String[] { "mp4" }, 30);
	}

	@Override
	protected CommonFile doStore(Path dest, String key, MultipartFile mf) throws LogicException {
		CommonFile file = super.doStore(dest, key, mf);
		try {
			extraPoster(dest, getPoster(key));
		} catch (Exception e) {
			FileUtils.deleteQuietly(dest);
			logger.error(e.getMessage(), e);
			throw new LogicException("video.corrupt", "不是正确的视频文件或者视频已经损坏");
		}
		return file;
	}

	@Override
	public boolean canStore(MultipartFile multipartFile) {
		String ext = FileUtils.getFileExtension(multipartFile.getOriginalFilename());
		return ext.isEmpty() ? false : Arrays.stream(allowExtensions).anyMatch(ext::equalsIgnoreCase);
	}

	@Override
	protected Optional<Resource> handleOriginalFile(Path path, HttpServletRequest request) {
		return Optional.of(new PathResource(path));
	}

	@Override
	protected synchronized void extraPoster(Path original, Path poster) throws Exception {
		Path temp = FileUtils.appTemp(FileUtils.getFileExtension(poster));
		String[] cmdArray = new String[] { "ffmpeg", "-loglevel", "error", "-y", "-ss", "00:00:01", "-i",
				original.toString(), "-vframes", "1", "-q:v", "2", temp.toString() };
		ProcessUtils.runProcess(cmdArray, timeoutSecond, TimeUnit.SECONDS);
		FileUtils.move(temp, poster);
	}

}
