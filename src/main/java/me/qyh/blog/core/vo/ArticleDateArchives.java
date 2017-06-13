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
package me.qyh.blog.core.vo;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import me.qyh.blog.core.exception.SystemException;

/**
 * 文档日期归档
 * 
 * @author Administrator
 *
 */
public class ArticleDateArchives {
	/**
	 * 文章日期归档模式
	 * 
	 * @author Administrator
	 *
	 */
	public enum ArticleDateFileMode {
		Y, YM, YMD
	}

	private List<ArticleDateArchive> archives = new ArrayList<>();
	private ArticleDateFileMode mode;

	/**
	 * 构造器
	 * 
	 * @param files
	 *            文章归档集合
	 * @param mode
	 *            归档模式
	 */
	public ArticleDateArchives(List<ArticleDateArchive> archives, ArticleDateFileMode mode) {
		this.archives = archives;
		this.mode = mode;
	}

	/**
	 * default
	 */
	public ArticleDateArchives() {
		super();
	}

	public ArticleDateFileMode getMode() {
		return mode;
	}

	public void setMode(ArticleDateFileMode mode) {
		this.mode = mode;
	}

	public List<ArticleDateArchive> getArchives() {
		return archives;
	}

	public void setArchives(List<ArticleDateArchive> archives) {
		this.archives = archives;
	}

	/**
	 * 根据模式计算归档的开始和结束日期
	 */
	public void calDate() {
		for (ArticleDateArchive archive : archives) {
			Calendar cal = Calendar.getInstance();
			cal.setTime(archive.getBegin());
			int year = cal.get(Calendar.YEAR);
			int month = cal.get(Calendar.MONTH);
			int day = cal.get(Calendar.DAY_OF_MONTH);
			cal.clear();
			cal.set(Calendar.YEAR, year);
			switch (mode) {
			case Y:
				archive.setBegin(cal.getTime());
				cal.add(Calendar.YEAR, 1);
				archive.setEnd(cal.getTime());
				break;
			case YM:
				cal.set(Calendar.MONTH, month);
				archive.setBegin(cal.getTime());
				cal.add(Calendar.MONTH, 1);
				archive.setEnd(cal.getTime());
				break;
			case YMD:
				cal.set(Calendar.MONTH, month);
				cal.set(Calendar.DAY_OF_MONTH, day);
				archive.setBegin(cal.getTime());
				cal.add(Calendar.DAY_OF_MONTH, 1);
				archive.setEnd(cal.getTime());
				break;
			default:
				throw new SystemException("无法识别的归档模式：" + mode);
			}
		}
	}
}
