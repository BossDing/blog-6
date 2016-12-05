package me.qyh.blog.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.lang3.RandomStringUtils;

import me.qyh.blog.exception.SystemException;

public class FileUtils {

	private static Lock fsLock = new ReentrantLock();

	private FileUtils() {

	}

	/**
	 * 采用随机6位前缀，创建一个临时空文件，<strong>需要手动删除！</strong>
	 * 
	 * @param ext
	 *            文件名后缀
	 * @return 临时文件
	 */
	public static File temp(String ext) {
		try {
			return File.createTempFile(RandomStringUtils.randomNumeric(6), "." + ext);
		} catch (IOException e) {
			throw new SystemException(e.getMessage(), e);
		}
	}

	public static boolean deleteQuietly(File file) {
		try {
			java.nio.file.Files.walkFileTree(file.toPath(), new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					java.nio.file.Files.delete(file);
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
					java.nio.file.Files.delete(dir);
					return FileVisitResult.CONTINUE;
				}
			});
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public static void main(String[] args) {
		FileUtils.deleteQuietly(new File("f:/123456.jpeg"));
	}

	/**
	 * 创建一个文件夹，如果失败，抛出异常
	 * 
	 * @param parentFile
	 */
	public static void forceMkdir(File dir) {
		if (dir.exists()) {
			return;
		}
		fsLock.lock();
		try {
			if (!dir.exists() && !dir.mkdirs())
				throw new SystemException("创建文件夹：" + dir.getAbsolutePath() + "失败");
		} finally {
			fsLock.unlock();
		}
	}

}
