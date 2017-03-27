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
package me.qyh.blog.core.thymeleaf;

import java.util.Objects;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.exceptions.TemplateProcessingException;
import org.thymeleaf.spring4.context.SpringContextUtils;

import me.qyh.blog.core.entity.Space;
import me.qyh.blog.core.exception.SystemException;
import me.qyh.blog.core.thymeleaf.template.Fragment;
import me.qyh.blog.core.thymeleaf.template.Page;
import me.qyh.blog.util.FileUtils;

public final class TemplateUtils {

	private static final String SPLITER = "%";
	private static final String TEMPLATE_PREFIX = "Template" + SPLITER;
	private static final String PAGE_PREFIX = TEMPLATE_PREFIX + "Page" + SPLITER;
	private static final String SYSTEM_PREFIX = TEMPLATE_PREFIX + "System" + SPLITER;
	private static final String PREVIEW = TEMPLATE_PREFIX + "Preview";
	private static final String FRAGMENT_PREFIX = TEMPLATE_PREFIX + "Fragment" + SPLITER;
	private static final String PATH_PREFIX = TEMPLATE_PREFIX + "Path" + SPLITER;

	/**
	 * 判断是否是pathTemplate
	 * 
	 * @param templateName
	 * @return
	 */
	public static boolean isPathTemplate(String templateName) {
		return templateName.startsWith(PATH_PREFIX);
	}

	/**
	 * 判断模板是否是页面模板
	 * 
	 * @param templateName
	 * @return
	 */
	public static boolean isPageTemplate(String templateName) {
		return templateName.startsWith(PAGE_PREFIX);
	}

	/**
	 * 判断模板是否是系统页面模板
	 * 
	 * @param templateName
	 * @return
	 */
	public static boolean isSystemTemplate(String templateName) {
		return templateName.startsWith(SYSTEM_PREFIX);
	}

	/**
	 * 判断模板是否是模板片段模板
	 * 
	 * @param templateName
	 * @return
	 */
	public static boolean isFragmentTemplate(String templateName) {
		return templateName.startsWith(FRAGMENT_PREFIX);
	}

	/**
	 * 判断是否是template
	 * 
	 * @param templateName
	 * @return
	 */
	public static boolean isTemplate(String templateName) {
		return templateName.startsWith(TEMPLATE_PREFIX);
	}

	/**
	 * 获取自定义页面的模板名
	 * 
	 * @param page
	 *            自定义页面
	 * @return
	 */
	public static String getPageTemplateName(Page page) {
		StringBuilder sb = new StringBuilder();
		sb.append(PAGE_PREFIX).append(cleanTemplatePath(page.getAlias()));
		Space space = page.getSpace();
		if (space != null && space.hasId()) {
			sb.append(SPLITER).append(space.getId());
		}
		return sb.toString();
	}

	/**
	 * 获取预览页面的模板名称
	 * 
	 * @return
	 */
	public static String getPreviewTemplateName() {
		return PREVIEW;
	}

	/**
	 * 判断页面是否预览页面
	 * 
	 * @param templateName
	 * @return
	 */
	public static boolean isPreview(String templateName) {
		return PREVIEW.equals(templateName);
	}

	/**
	 * 获取模板片段的模板名
	 * 
	 * @param fragment
	 *            模板片段
	 * @return
	 */
	public static String getFragmentTemplateName(Fragment fragment) {
		StringBuilder sb = new StringBuilder();
		sb.append(FRAGMENT_PREFIX).append(fragment.getName());
		Space space = fragment.getSpace();
		if (space != null && space.hasId()) {
			sb.append(SPLITER).append(space.getId());
		}
		return sb.toString();
	}

	/**
	 * templateName to fragment
	 * 
	 * @param templateName
	 * @return
	 */
	public static Fragment toFragment(String templateName) {
		String[] array = templateName.split(SPLITER);
		if (array.length == 3) {
			return new Fragment(array[2]);
		}
		if (array.length == 4) {
			return new Fragment(array[2], new Space(Integer.parseInt(array[3])));
		}
		throw new SystemException(templateName + "无法转化为Fragment");
	}

	/**
	 * 从pathTemplate的templateName中获取path
	 * 
	 * @return
	 */
	public static PathTemplateInfo parsePathTemplateName(String templateName) {
		// Template%Path%path
		// Template%Path%
		// Template%Path%%space
		// Template%Path%path%space
		String[] array = templateName.split(SPLITER);
		if (array.length == 2) {
			return new PathTemplateInfo("", null);
		}
		if (array.length == 3) {
			return new PathTemplateInfo(array[2], null);
		}
		if (array.length == 4) {
			return new PathTemplateInfo(array[2], array[3]);
		}
		throw new SystemException("无法从" + templateName + "中获取路径");
	}

	/**
	 * 从SystemTemplate的templateName中获取path
	 * 
	 * @return
	 */
	public static String getPathFromSystemTemplateName(String templateName) {
		String[] array = templateName.split(SPLITER);
		if (array.length == 3) {
			return array[2];
		}
		if (array.length == 2) {
			return "";
		}
		throw new SystemException("无法从" + templateName + "中获取路径");
	}

	/**
	 * 
	 * @param pathTemplatePath
	 * @return
	 */
	public static String getPathTemplateName(String relativePath, String spaceAlias) {
		String templateName = PATH_PREFIX + cleanTemplatePath(relativePath);
		if (spaceAlias != null) {
			templateName += SPLITER + spaceAlias;
		}
		return templateName;
	}

	/**
	 * 获取系统模板的
	 * 
	 * @param systemTemplatePath
	 * @return
	 */
	public static String getSystemTemplateName(String systemTemplatePath) {
		return SYSTEM_PREFIX + cleanTemplatePath(systemTemplatePath);
	}

	/**
	 * 
	 * @param templateName
	 * @return
	 */
	public static Page toPage(String templateName) {
		String[] array = templateName.split(SPLITER);
		if (array.length == 2) {
			return new Page("");
		}
		if (array.length == 3) {
			return new Page(array[2]);
		}
		if (array.length == 4) {
			return new Page(new Space(Integer.parseInt(array[3])), cleanTemplatePath(array[2]));
		}
		throw new SystemException(templateName + "无法转化为用户自定义页面");
	}

	/**
	 * 获取spring bean
	 * 
	 * @param t
	 * @throws TemplateProcessingException
	 *             如果bean不存在或者ApplicationContext为null
	 * @return
	 */
	public static <T> T getRequireBean(ITemplateContext context, Class<T> t) {
		Objects.requireNonNull(context);
		Objects.requireNonNull(t);
		ApplicationContext ctx = SpringContextUtils.getApplicationContext(context);
		if (ctx != null) {
			try {
				return ctx.getBean(t);
			} catch (BeansException e) {
				throw new TemplateProcessingException(e.getMessage(), e);
			}
		}
		throw new TemplateProcessingException("ApplicationContext为null");
	}

	/**
	 * 
	 * @param path
	 * @return
	 */
	public static String cleanTemplatePath(String path) {
		return FileUtils.cleanPath(path);
	}

	public static final class PathTemplateInfo {
		private final String path;
		private final String spaceAlias;

		private PathTemplateInfo(String path, String spaceAlias) {
			super();
			this.path = path;
			this.spaceAlias = spaceAlias;
		}

		public String getPath() {
			return path;
		}

		public String getSpaceAlias() {
			return spaceAlias;
		}
		
	}
}
