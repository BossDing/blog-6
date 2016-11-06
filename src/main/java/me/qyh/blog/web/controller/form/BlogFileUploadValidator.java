package me.qyh.blog.web.controller.form;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.web.multipart.MultipartFile;

@Component
public class BlogFileUploadValidator implements Validator {

	public static final int MAX_FILE_NAME_LENGTH = 500;

	@Override
	public boolean supports(Class<?> clazz) {
		return BlogFileUpload.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		BlogFileUpload upload = (BlogFileUpload) target;
		List<MultipartFile> files = upload.getFiles();
		if (CollectionUtils.isEmpty(files)) {
			errors.reject("file.uploadfiles.blank", "需要上传文件为空");
			return;
		}
		for (MultipartFile file : files) {
			if (file.getOriginalFilename().length() > MAX_FILE_NAME_LENGTH) {
				errors.reject("file.name.toolong", new Object[] { MAX_FILE_NAME_LENGTH },
						"文件名不能超过" + MAX_FILE_NAME_LENGTH + "个字符");
				return;
			}
			if (file.isEmpty()) {
				errors.reject("file.content.blank", "文件内容不能为空");
				return;
			}
		}
		if (upload.getServer() == null) {
			errors.reject("file.uploadserver.blank", "文件上传服务为空");
			return;
		}
	}

}
