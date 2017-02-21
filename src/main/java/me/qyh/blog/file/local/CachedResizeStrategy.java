package me.qyh.blog.file.local;

import static me.qyh.blog.file.ImageHelper.JPEG;
import static me.qyh.blog.file.ImageHelper.PNG;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import me.qyh.blog.exception.SystemException;
import me.qyh.blog.file.ImageHelper;
import me.qyh.blog.file.Resize;
import me.qyh.blog.file.local.ImageResourceStore.ResizeStrategy;
import me.qyh.blog.util.FileUtils;

/**
 * 用于防止同时生成相同的缩略图以及封面
 * <p>
 * <b>当图片被替换时(路径不变)，可能有长达一分钟的延迟(缓存原因)</b>
 * </p>
 * 
 * @author mhlx
 *
 */
public class CachedResizeStrategy implements ResizeStrategy, InitializingBean {

	@Autowired
	private ImageHelper imageHelper;
	@Autowired
	private ThreadPoolTaskScheduler taskScheduler;

	private final Map<String, Long> fileMap = new ConcurrentHashMap<>();
	private final Map<String, Long> coverMap = new ConcurrentHashMap<>();

	/**
	 * 数据有效期60s
	 */
	private static final long LIVE_MILL = 60 * 1000L;

	@Override
	public void doResize(File local, File thumb, Resize resize) throws IOException {
		String ext = FileUtils.getFileExtension(local.getName());
		String coverExt = "." + (ImageHelper.maybeTransparentBg(ext) ? PNG : JPEG);
		File cover = new File(thumb.getParentFile(), FileUtils.getNameWithoutExtension(local.getName()) + coverExt);

		String coverCanonicalPath = cover.getCanonicalPath();
		String thumbCanonicalPath = thumb.getCanonicalPath();

		long now = System.currentTimeMillis();
		fileMap.compute(thumbCanonicalPath, (k, v) -> {
			if (v == null || (now - v > LIVE_MILL)) {
				if (!thumb.exists()) {
					coverMap.compute(coverCanonicalPath, (ck, cv) -> {
						if (cv == null || (now - cv > LIVE_MILL)) {
							if (!cover.exists()) {
								FileUtils.forceMkdir(cover.getParentFile());
								try {
									imageHelper.format(local, cover);
								} catch (IOException e) {
									if (local.exists()) {
										throw new SystemException(e.getMessage(), e);
									}
								}
							}
							return now;
						}
						return cv;
					});
				}
				FileUtils.forceMkdir(thumb.getParentFile());
				try {
					imageHelper.resize(resize, cover, thumb);
				} catch (IOException e) {
					if (cover.exists()) {
						throw new SystemException(e.getMessage(), e);
					}
				}
				return now;
			}
			return v;
		});
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		taskScheduler.scheduleAtFixedRate(() -> {
			long now = System.currentTimeMillis();
			fileMap.forEach((k, v) -> {
				if ((now - v) > LIVE_MILL) {
					fileMap.remove(k);
				}
			});
			coverMap.forEach((k, v) -> {
				if ((now - v) > LIVE_MILL) {
					fileMap.remove(k);
				}
			});
		}, LIVE_MILL);
	}

}
