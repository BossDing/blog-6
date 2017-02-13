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
package me.qyh.blog.web.controller.form;

import java.io.File;

import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import me.qyh.blog.entity.BlogFile;
import me.qyh.blog.service.FileService;
import me.qyh.blog.util.Validators;

@Component
public class BlogFileValidator implements Validator {

	public static final int MAX_PATH_LENGTH = 30;
	public static final int MAX_FILE_NAME_LENGTH = 225;

	private static final String PATH_PATTERN = "^[A-Za-z0-9_-\u4E00-\u9FA5]+$";

	@Override
	public boolean supports(Class<?> clazz) {
		return BlogFile.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		BlogFile file = (BlogFile) target;
		if (!file.hasId()) {
			if (file.isDir()) {
				String path = file.getPath();
				if (Validators.isEmptyOrNull(path, true)) {
					errors.reject("file.path.blank", "文件夹路径不能为空");
					return;
				}
				if (path.length() > MAX_PATH_LENGTH) {
					errors.reject("file.path.toolong", new Object[] { path, MAX_PATH_LENGTH },
							"路径" + path + "不能超过" + MAX_PATH_LENGTH + "个字符");
					return;
				}
				if (!checkPath(path)) {
					errors.reject("file.path.valid", new Object[] { path }, "文件名" + path + "无效，文件名必须为字母数字或者汉字或者_和-");
					return;
				}
			}
		} else {
			if (file.isFile()) {
				String path = file.getPath();
				if (Validators.isEmptyOrNull(path, true)) {
					errors.reject("file.path.blank", "文件夹路径不能为空");
					return;
				}
				String fileName;
				if (path.indexOf(FileService.SPLIT_CHAR) == -1) {
					fileName = path;
				} else {
					String[] toCopy = path.split(FileService.SPLIT_CHAR);
					String[] pathArray = new String[toCopy.length - 1];
					System.arraycopy(toCopy, 0, pathArray, 0, toCopy.length - 1);

					for (String _path : pathArray) {
						if (_path.isEmpty()) {
							continue;
						}
						if (_path.length() > MAX_PATH_LENGTH) {
							errors.reject("file.path.toolong", new Object[] { _path, MAX_PATH_LENGTH },
									"路径" + _path + "不能超过" + MAX_PATH_LENGTH + "个字符");
							return;
						}
						if (!checkPath(_path)) {
							errors.reject("file.path.valid", new Object[] { _path }, "文件名" + _path + "无效，文件名必须为字母数字或者汉字或者_和-");
							return;
						}
					}

					fileName = toCopy[toCopy.length - 1];
				}

				if (fileName.length() > MAX_FILE_NAME_LENGTH) {
					errors.reject("file.name.toolong", new Object[] { fileName, MAX_FILE_NAME_LENGTH },
							"文件名不能超过" + MAX_FILE_NAME_LENGTH + "个字符");
					return;
				}

				if (!checkPath(fileName)) {
					errors.reject("file.path.valid", new Object[] { fileName },
							"文件名" + fileName + "无效，文件名必须为字母数字或者汉字或者_和-");
					return;
				}

			}
		}
	}

	public static boolean checkPath(String path) {
		if (path.matches(PATH_PATTERN)) {
			try {
				new File(path).getCanonicalPath();
				return true;
			} catch (Exception e) {
				return false;
			}
		}
		return false;
	}
}
