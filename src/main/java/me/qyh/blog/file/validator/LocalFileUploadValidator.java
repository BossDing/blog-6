package me.qyh.blog.file.validator;

import java.util.List;

import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.web.multipart.MultipartFile;

import me.qyh.blog.file.vo.LocalFileUpload;

@Component
public class LocalFileUploadValidator implements Validator {

	@Override
	public boolean supports(Class<?> clazz) {
		return LocalFileUpload.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		LocalFileUpload upload = (LocalFileUpload) target;
		List<MultipartFile> files = upload.getFiles();
		if (CollectionUtils.isEmpty(files)) {
			errors.reject("file.uploadfiles.blank", "需要上传文件为空");
			return;
		}
		for (MultipartFile file : files) {

			if (file.isEmpty()) {
				errors.reject("file.content.blank", "文件内容不能为空");
				return;
			}
		}
	}
	
}
