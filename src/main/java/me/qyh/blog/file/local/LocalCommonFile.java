package me.qyh.blog.file.local;

import java.io.File;

import me.qyh.blog.file.CommonFile;

public class LocalCommonFile extends CommonFile {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private File file;

	public File getFile() {
		return file;
	}

	public void setFile(File file) {
		this.file = file;
	}
}