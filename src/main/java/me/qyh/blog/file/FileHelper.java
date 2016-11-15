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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.lang3.RandomStringUtils;

import me.qyh.blog.exception.SystemException;

/**
 * 文件辅助类
 * 
 * @author Administrator
 *
 */
public final class FileHelper {

	private FileHelper() {

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
			return File.createTempFile(RandomStringUtils.random(6), "." + ext);
		} catch (IOException e) {
			throw new SystemException(e.getMessage(), e);
		}
	}

	/**
	 * 开启文件流
	 * 
	 * @param file
	 *            文件
	 * @return 文件流
	 */
	public static InputStream openStream(File file) {
		try {
			return new FileInputStream(file);
		} catch (FileNotFoundException e) {
			throw new SystemException(e.getMessage(), e);
		}
	}
}
