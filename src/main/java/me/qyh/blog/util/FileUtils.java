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
package me.qyh.blog.util;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;
import java.util.function.Predicate;

import me.qyh.blog.exception.SystemException;
import me.qyh.blog.service.impl.FileClearJob;

public class FileUtils {

	private FileUtils() {

	}

	private static final File HOME_DIR = new File(System.getProperty("user.home"));

	/**
	 * 博客用来存放临时文件的文件夹
	 */
	private static final File TEMP_DIR = new File(HOME_DIR, "blog_temp");

	static {
		forceMkdir(TEMP_DIR);
	}

	/**
	 * 采用UUID创造一个文件，这个文件为系统临时文件，会通过定时任务删除
	 * 
	 * @param ext
	 *            文件后缀
	 * @return 临时文件
	 * @see FileUtils#clearAppTemp(Predicate)
	 * @see FileClearJob#doJob()
	 */
	public static File appTemp(String ext) {
		String name = StringUtils.uuid() + "." + ext;
		try {
			File temp = new File(TEMP_DIR, name);
			Files.createFile(temp.toPath());
			return temp;
		} catch (IOException e) {
			throw new SystemException(e.getMessage(), e);
		}
	}

	/**
	 * write bits to file
	 * 
	 * @param bytes
	 * @param file
	 * @throws IOException
	 */
	public static void write(byte[] bytes, File file) throws IOException {
		Files.write(file.toPath(), bytes, StandardOpenOption.WRITE);
	}

	/**
	 * 获取文件的后缀名
	 * 
	 * @param fullName
	 * @return
	 */
	public static String getFileExtension(String fullName) {
		String fileName = new File(fullName).getName();
		int dotIndex = fileName.lastIndexOf('.');
		return (dotIndex == -1) ? "" : fileName.substring(dotIndex + 1);
	}

	/**
	 * 获取文件名(不包括后缀)
	 * 
	 * @param file
	 * @return
	 */
	public static String getNameWithoutExtension(String file) {
		String fileName = new File(file).getName();
		int dotIndex = fileName.lastIndexOf('.');
		return (dotIndex == -1) ? fileName : fileName.substring(0, dotIndex);
	}

	/**
	 * 删除文件|文件夹
	 * 
	 * @param file
	 * @return
	 */
	public static boolean deleteQuietly(File file) {
		if (file == null || !file.exists()) {
			return true;
		}
		try {
			Files.walkFileTree(file.toPath(), new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					Files.delete(file);
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
					Files.delete(dir);
					return FileVisitResult.CONTINUE;
				}
			});
			return true;
		} catch (Exception e) {
			return false;
		}
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
		synchronized (FileUtils.class) {
			if (!dir.exists() && !dir.mkdirs()) {
				throw new SystemException("创建文件夹：" + dir.getAbsolutePath() + "失败");
			}
		}
	}

	/**
	 * 拷贝一个文件
	 * 
	 * @param source
	 * @param target
	 * @throws IOException
	 */
	public static void copy(File source, File target) throws IOException {
		forceMkdir(target.getParentFile());
		Files.copy(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
	}

	/**
	 * 移动一个文件
	 * 
	 * @param png
	 * @param dest
	 * @throws IOException
	 */
	public static void move(File source, File target) throws IOException {
		forceMkdir(target.getParentFile());
		Files.move(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
	}

	/**
	 * write file to outputstream
	 * 
	 * @param file
	 * @param outputStream
	 * @throws IOException
	 */
	public static void write(File file, OutputStream outputStream) throws IOException {
		Files.copy(file.toPath(), outputStream);
		outputStream.flush();
	}

	/**
	 * 获取主目录
	 * 
	 * @return
	 */
	public static File getHomeDir() {
		return HOME_DIR;
	}

	/**
	 * 删除系统临时文件夹内符合条件的文件
	 * 
	 * @param predicate
	 */
	public static void clearAppTemp(Predicate<Path> predicate) {
		Objects.requireNonNull(predicate);
		try {
			Files.walkFileTree(TEMP_DIR.toPath(), new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {

					if (predicate.test(file)) {
						try {
							Files.delete(file);
						} catch (Exception e) {
						}
					}
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
					return FileVisitResult.CONTINUE;
				}
			});
		} catch (IOException e) {
			// ignore
		}
	}
}
