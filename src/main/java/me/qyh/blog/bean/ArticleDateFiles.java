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
