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
package me.qyh.blog.core.ui.data;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;

import me.qyh.blog.core.entity.BlogFile;
import me.qyh.blog.core.exception.LogicException;
import me.qyh.blog.core.pageparam.BlogFileQueryParam;
import me.qyh.blog.core.pageparam.PageResult;
import me.qyh.blog.core.service.FileService;
import me.qyh.blog.util.Validators;

public class FilesDataTagProcessor extends DataTagProcessor<PageResult<BlogFile>> {

	private static final String PATH = "path";
	private static final String EXTENSIONS = "extensions";

	private static final int MAX_EXTENSION_LENGTH = 5;

	@Autowired
	private FileService fileService;

	public FilesDataTagProcessor(String name, String dataName) {
		super(name, dataName);
	}

	@Override
	protected PageResult<BlogFile> buildPreviewData(Attributes attributes) {
		BlogFileQueryParam param = new BlogFileQueryParam();
		param.setCurrentPage(1);
		param.setPageSize(10);
		return new PageResult<>(param, 0, Collections.emptyList());
	}

	@Override
	protected PageResult<BlogFile> query(Attributes attributes) throws LogicException {

		int currentPage = 0;

		String currentPageStr = attributes.get(Constants.CURRENT_PAGE);
		if (currentPageStr != null) {
			try {
				currentPage = Integer.parseInt(currentPageStr);
			} catch (Exception e) {
				LOGGER.debug(e.getMessage(), e);
			}
		}
		if (currentPage <= 0) {
			currentPage = 1;
		}

		Set<String> extensions = null;
		String extensionStr = attributes.get(EXTENSIONS);
		if (!Validators.isEmptyOrNull(extensionStr, true)) {
			extensions = Arrays.stream(extensionStr.split(",")).map(ext -> ext.trim())
					.filter(ext -> !ext.trim().isEmpty()).limit(MAX_EXTENSION_LENGTH).collect(Collectors.toSet());
		}

		BlogFileQueryParam param = new BlogFileQueryParam();
		param.setCurrentPage(currentPage);
		param.setExtensions(extensions);

		String pageSizeStr = attributes.get(Constants.PAGE_SIZE);
		if (pageSizeStr != null) {
			try {
				param.setPageSize(Integer.parseInt(pageSizeStr));
			} catch (Exception e) {
				LOGGER.debug(e.getMessage(), e);
			}
		}

		String path = attributes.get(PATH);
		return fileService.queryFiles(path, param);
	}
}
