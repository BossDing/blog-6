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
package me.qyh.blog.service.impl;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Autowired;

import me.qyh.blog.service.FileService;
import me.qyh.blog.util.FileUtils;

public class FileClearJob {

	private static final long MAX_MODIFY_TIME = 30 * 60 * 1000;

	@Autowired
	private FileService fileService;

	public void doJob() {
		fileService.clearDeletedCommonFile();
		FileUtils.clearAppTemp(this::overMaxModifyTime);
	}

	public boolean overMaxModifyTime(Path path) {
		try {
			long lastModifyMill = Files.getLastModifiedTime(path).toMillis();
			return System.currentTimeMillis() - lastModifyMill > MAX_MODIFY_TIME;
		} catch (IOException e) {
			return true;
		}
	}

}
