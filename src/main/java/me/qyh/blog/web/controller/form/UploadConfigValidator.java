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
import me.qyh.blog.util.FileUtils;
import me.qyh.blog.util.Validators;

@Component
public class UploadConfigValidator implements Validator {

	@Override
	public boolean supports(Class<?> clazz) {
		return UploadConfig.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		UploadConfig config = (UploadConfig) target;
		String path = config.getPath();
		if (!Validators.isEmptyOrNull(path, true)) {
			path = FileUtils.cleanPath(path);
			BlogFileValidator.validFolderPath(path, errors);
			if (errors.hasErrors()) {
				return;
			}
			config.setPath(path);
		}
		if (config.getStore() == null) {
			errors.reject("file.uploadstore.blank", "文件存储器为空");
			return;
		}
	}

}
