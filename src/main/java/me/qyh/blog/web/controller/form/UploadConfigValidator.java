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

import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import me.qyh.blog.config.UploadConfig;
import me.qyh.blog.service.FileService;
import me.qyh.blog.util.Validators;

@Component
public class UploadConfigValidator implements Validator {

	private static final int MAX_PATH_LENGTH = BlogFileValidator.MAX_PATH_LENGTH;

	private static final int MAX_FLOOR = 5;

	@Override
	public boolean supports(Class<?> clazz) {
		return UploadConfig.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		UploadConfig config = (UploadConfig) target;
		String path = config.getPath();
		if (!Validators.isEmptyOrNull(path, true)) {
			if (path.indexOf(FileService.SPLIT_CHAR) != -1) {
				String[] pathArray = path.split(FileService.SPLIT_CHAR);
				if (pathArray.length > MAX_FLOOR) {
					errors.reject("uploadConfig.floor.oversize", new Object[] { MAX_FLOOR },
							"路径最多只能" + MAX_FLOOR + "层");
					return;
				}
				StringBuilder sb = new StringBuilder();
				for (String _path : pathArray) {
					_path = _path.trim();
					if (!_path.isEmpty()) {
						validatePath(_path, errors);
						if (errors.hasErrors()) {
							return;
						}
						sb.append(_path).append(FileService.SPLIT_CHAR);
					}
				}
				if (sb.length() > 0)
					sb.deleteCharAt(sb.length() - 1);
				config.setPath(path);
			} else {
				validatePath(path, errors);
			}
		}
		if (config.getStore() == null) {
			errors.reject("file.uploadstore.blank", "文件存储器为空");
			return;
		}
	}

	private void validatePath(String path, Errors errors) {
		if (path.length() > MAX_PATH_LENGTH) {
			errors.reject("file.path.toolong", new Object[] { MAX_PATH_LENGTH }, "文件夹路径不能超过" + MAX_PATH_LENGTH + "个字符");
			return;
		}
		if (!BlogFileValidator.checkPath(path)) {
			errors.reject("file.path.valid", "文件夹路径无效");
			return;
		}
	}

}
