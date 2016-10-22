package me.qyh.blog.bean;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * 文档日期归档
 * 
 * @author Administrator
 *
 */
public class ArticleDateFiles {
	public enum ArticleDateFileMode {
		Y, YM, YMD
	}

	private List<ArticleDateFile> files = new ArrayList<ArticleDateFile>();
	private ArticleDateFileMode mode;

	public ArticleDateFileMode getMode() {
		return mode;
	}

	public void setMode(ArticleDateFileMode mode) {
		this.mode = mode;
	}

	public List<ArticleDateFile> getFiles() {
		return files;
	}

	public void setFiles(List<ArticleDateFile> files) {
		this.files = files;
	}

	public ArticleDateFiles(List<ArticleDateFile> files, ArticleDateFileMode mode) {
		this.files = files;
		this.mode = mode;
	}

	public void calDate() {
		for (ArticleDateFile file : files) {
			Calendar cal = Calendar.getInstance();
			cal.setTime(file.getBegin());
			int year = cal.get(Calendar.YEAR);
			int month = cal.get(Calendar.MONTH);
			int day = cal.get(Calendar.DAY_OF_MONTH);
			cal.clear();
			cal.set(Calendar.YEAR, year);
			switch (mode) {
			case Y:
				file.setBegin(cal.getTime());
				cal.add(Calendar.YEAR, 1);
				file.setEnd(cal.getTime());
				break;
			case YM:
				cal.set(Calendar.MONTH, month);
				file.setBegin(cal.getTime());
				cal.add(Calendar.MONTH, 1);
				file.setEnd(cal.getTime());
				break;
			case YMD :
				cal.set(Calendar.MONTH, month);
				cal.set(Calendar.DAY_OF_MONTH, day);
				file.setBegin(cal.getTime());
				cal.add(Calendar.DAY_OF_MONTH, 1);
				file.setEnd(cal.getTime());
				break;
			}
		}
	}

	public ArticleDateFiles() {

	}

	public void addArticleDateFile(ArticleDateFile file) {
		files.add(file);
	}

}
