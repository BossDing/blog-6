package me.qyh.blog.file.store.local;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import me.qyh.blog.core.exception.SystemException;
import me.qyh.blog.core.util.Resources;
import me.qyh.blog.core.util.Validators;

/**
 * 用于处理视频文件
 * <p>
 * <b>需要在环境变量中注册ffmpeg的bin目录</b>
 * </p>
 * 
 * @author wwwqyhme
 *
 */
class FFmpegUtils {

	/**
	 * 获取视频封面
	 * 
	 * @param video 视频地址	
	 * @param dest 封面地址
	 */
	public static void extraPoster(Path video, Path dest, long time, TimeUnit unit) throws FFmpegException {
		String[] cmdArray = new String[] { "ffmpeg", "-loglevel", "error", "-y", "-ss", "00:00:01", "-i",
				video.toString(), "-vframes", "1", "-q:v", "2", dest.toString() };
		ProcessBuilder builder = new ProcessBuilder(cmdArray);
		Process process;
		try {
			process = builder.start();
		} catch (IOException e) {
			throw new FFmpegException(e.getMessage(), e);
		}

		try {
			if (!process.waitFor(time, unit)) {
				destory(process);
				throw new FFmpegException("获取视频" + video + "超时");
			}
		} catch (InterruptedException e) {
			destory(process);
			Thread.currentThread().interrupt();
			throw new SystemException(e.getMessage(), e);
		}

		int status = process.exitValue();
		if (status != 0) {
			try (InputStream error = process.getErrorStream()) {
				if (error != null) {
					String msg = Resources.read(error);
					if (!Validators.isEmptyOrNull(msg, true)) {
						throw new FFmpegException(msg);
					}
				}
			} catch (IOException e) {
				throw new FFmpegException("读取错误信息失败：" + e.getMessage(), e);
			}
			throw new FFmpegException("未正确执行操作，状态码:" + status);
		}
	}

	private static void destory(Process p) {
		p.destroy();
	}

}
