package me.qyh.blog.bean;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import me.qyh.blog.ui.page.Page;

public class ImportResult {

	// 原始页面，用于恢复
	private List<Page> oldPages = new ArrayList<Page>();
	// 导入过程中出错
	private List<ImportError> errors = new ArrayList<ImportError>();
	// 导入成功的页面序号
	private List<ImportSuccess> successes = new ArrayList<ImportSuccess>();

	public List<Page> getOldPages() {
		return oldPages;
	}

	public void setOldPages(List<Page> oldPages) {
		this.oldPages = oldPages;
	}

	public List<ImportError> getErrors() {
		return errors;
	}

	public void setErrors(List<ImportError> errors) {
		this.errors = errors;
	}

	public List<ImportSuccess> getSuccesses() {
		return successes;
	}

	public void setSuccesses(List<ImportSuccess> successes) {
		this.successes = successes;
	}

	public void addOldPage(Page old) {
		oldPages.add(old);
	}

	public void addError(ImportError error) {
		errors.add(error);
	}

	public void addSuccess(ImportSuccess success) {
		successes.add(success);
	}

	public void addErrors(List<ImportError> errors) {
		this.errors.addAll(errors);
	}
	
	public void sort(){
		Collections.sort(errors);
	}

}
