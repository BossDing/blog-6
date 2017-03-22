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
package me.qyh.blog.core.ui;

import java.util.Objects;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.exceptions.TemplateProcessingException;
import org.thymeleaf.spring4.context.SpringContextUtils;

import me.qyh.blog.core.entity.Space;
import me.qyh.blog.core.exception.SystemException;
import me.qyh.blog.core.ui.fragment.Fragment;
import me.qyh.blog.core.ui.page.LockPage;
import me.qyh.blog.core.ui.page.Page;
import me.qyh.blog.core.ui.page.SysPage;
import me.qyh.blog.core.ui.page.SysPage.PageTarget;
import me.qyh.blog.core.ui.page.UserPage;
import me.qyh.blog.util.FileUtils;

public final class TemplateUtils {

	private static final String SPLITER = "%";
	private static final String TEMPLATE_PREFIX = "Template" + SPLITER;
	private static final String SYSPAGE_PREFIX = TEMPLATE_PREFIX + "Page:Sys" + SPLITER;
	private static final String USERPAGE_PREFIX = TEMPLATE_PREFIX + "Page:User" + SPLITER;
	private static final String LOCKPAGE_PREFIX = TEMPLATE_PREFIX + "Page:Lock" + SPLITER;
	private static final String PREVIEW = TEMPLATE_PREFIX + "Preview";
	private static final String FRAGMENT_PREFIX = TEMPLATE_PREFIX + "Fragment" + SPLITER;

	/**
	 * 根据页面模板名转化为对应的页面
	 * 
	 * @param templateName
	 *            模板名
	 * @return
	 */
	public static Page toPage(String templateName) {
		if (templateName.startsWith(SYSPAGE_PREFIX)) {
			return toSysPage(templateName);
		}
		if (templateName.startsWith(USERPAGE_PREFIX)) {
			return toUserPage(templateName);
		}
		if (templateName.startsWith(LOCKPAGE_PREFIX)) {
			return toLockPage(templateName);
		}
		throw new SystemException(templateName + "无法转化为具体的页面");
	}

	/**
	 * 判断模板是否是页面模板
	 * 
	 * @param templateName
	 * @return
	 */
	public static boolean isPageTemplate(String templateName) {
		return templateName.startsWith(TEMPLATE_PREFIX + "Page:");
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
	 * @param userPage
	 *            自定义页面
	 * @return
	 */
	public static String getTemplateName(UserPage userPage) {
		StringBuilder sb = new StringBuilder();
		sb.append(USERPAGE_PREFIX).append(cleanUserPageAlias(userPage.getAlias()));
		Space space = userPage.getSpace();
		if (space != null && space.hasId()) {
			sb.append(SPLITER).append(space.getId());
		}
		return sb.toString();
	}

	/**
	 * 获取系统页面的模板名
	 * 
	 * @param sysPage
	 *            系统页面
	 * @return
	 */
	public static String getTemplateName(SysPage sysPage) {
		StringBuilder sb = new StringBuilder();
		sb.append(SYSPAGE_PREFIX);
		sb.append(sysPage.getTarget().name());
		Space space = sysPage.getSpace();
		if (space != null && space.hasId()) {
			sb.append(SPLITER).append(space.getId());
		}
		return sb.toString();
	}

	/**
	 * 获取解锁页面模板名
	 * 
	 * @param lockPage
	 *            解锁页面
	 * @return
	 */
	public static String getTemplateName(LockPage lockPage) {
		StringBuilder sb = new StringBuilder();
		sb.append(LOCKPAGE_PREFIX).append(lockPage.getLockType());
		Space space = lockPage.getSpace();
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
	public static String getTemplateName(Fragment fragment) {
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

	private static LockPage toLockPage(String templateName) {
		String[] array = templateName.split(SPLITER);
		if (array.length == 3) {
			return new LockPage(array[2]);
		}
		if (array.length == 4) {
			return new LockPage(new Space(Integer.parseInt(array[3])), array[2]);
		}
		throw new SystemException(templateName + "无法转化为解锁页面");
	}

	private static UserPage toUserPage(String templateName) {
		String[] array = templateName.split(SPLITER);
		if (array.length == 3) {
			return new UserPage(array[2]);
		}
		if (array.length == 4) {
			return new UserPage(new Space(Integer.parseInt(array[3])), cleanUserPageAlias(array[2]));
		}
		throw new SystemException(templateName + "无法转化为用户自定义页面");
	}

	private static SysPage toSysPage(String templateName) {
		String[] array = templateName.split(SPLITER);
		if (array.length == 3) {
			return new SysPage(PageTarget.valueOf(array[2]));
		}
		if (array.length == 4) {
			return new SysPage(new Space(Integer.parseInt(array[3])), PageTarget.valueOf(array[2]));
		}
		throw new SystemException(templateName + "无法转化为系统页面");
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
	 * 清理自定义页面路径 删除连续的 '/'以及首尾的 '/'
	 * 
	 * @param alias
	 * @return
	 */
	public static String cleanUserPageAlias(String alias) {
		return FileUtils.cleanPath(alias);
	}
}
