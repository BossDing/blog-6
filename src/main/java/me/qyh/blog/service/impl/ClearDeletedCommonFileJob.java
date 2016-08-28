package me.qyh.blog.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import me.qyh.blog.service.FileService;

@Component
public class ClearDeletedCommonFileJob {

	@Autowired
	private FileService fileService;

	public void doJob() {
		fileService.clearDeletedCommonFile();
	}

}
