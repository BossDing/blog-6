package me.qyh.blog.ui.data;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;

import me.qyh.blog.entity.BlogFile;
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.pageparam.BlogFileQueryParam;
import me.qyh.blog.pageparam.PageResult;
import me.qyh.blog.service.FileService;
import me.qyh.blog.ui.ContextVariables;
import me.qyh.blog.util.Validators;

public class FilesDataTagProcessor extends DataTagProcessor<PageResult<BlogFile>> {

	private static final String PATH = "path";
	private static final String PAGE = "currentPage";
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
	protected PageResult<BlogFile> query(ContextVariables variables, Attributes attributes) throws LogicException {

		int currentPage = 0;

		String currentPageStr = super.getVariables(PAGE, variables, attributes);
		if (currentPageStr != null) {
			try {
				currentPage = Integer.parseInt(currentPageStr);
			} catch (Exception e) {
			}
		}
		if (currentPage <= 0) {
			currentPage = 1;
		}

		Set<String> extensions = null;
		String extensionStr = super.getVariables(EXTENSIONS, variables, attributes);
		if (!Validators.isEmptyOrNull(extensionStr, true)) {
			extensions = Arrays.stream(extensionStr.split(",")).map(ext -> ext.trim())
					.filter(ext -> !ext.trim().isEmpty()).limit(MAX_EXTENSION_LENGTH).collect(Collectors.toSet());
		}

		String path = super.getVariables(PATH, variables, attributes);
		return fileService.queryFiles(path, extensions, currentPage);
	}
}
