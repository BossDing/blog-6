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
package me.qyh.blog.ui;

import java.util.Map;
import java.util.Objects;

import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Tag;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.util.CollectionUtils;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.exceptions.TemplateProcessingException;
import org.thymeleaf.spring4.context.SpringContextUtils;

import me.qyh.blog.entity.Space;
import me.qyh.blog.exception.SystemException;
import me.qyh.blog.ui.fragment.Fragment;
import me.qyh.blog.ui.fragment.UserFragment;
import me.qyh.blog.ui.page.ErrorPage;
import me.qyh.blog.ui.page.ErrorPage.ErrorCode;
import me.qyh.blog.ui.page.LockPage;
import me.qyh.blog.ui.page.Page;
import me.qyh.blog.ui.page.SysPage;
import me.qyh.blog.ui.page.SysPage.PageTarget;
import me.qyh.blog.ui.page.UserPage;
import me.qyh.blog.util.FileUtils;

public final class TemplateUtils {

	private static final String SPLITER = "%";
	private static final String SYSPAGE_PREFIX = "Page:Sys" + SPLITER;
	private static final String USERPAGE_PREFIX = "Page:User" + SPLITER;
	private static final String LOCKPAGE_PREFIX = "Page:Lock" + SPLITER;
	private static final String ERRORPAGE_PREFIX = "Page:Error" + SPLITER;

	public static final String FRAGMENT_PREFIX = "Fragment" + SPLITER;

	/**
	 * 根据页面模板名转化为对应的页面
	 * 
	 * @param templateName
	 *            模板名
	 * @return
	 */
	public static Page convert(String templateName) {
		if (templateName.startsWith(SYSPAGE_PREFIX)) {
			return toSysPage(templateName);
		}
		if (templateName.startsWith(USERPAGE_PREFIX)) {
			return toUserPage(templateName);
		}
		if (templateName.startsWith(LOCKPAGE_PREFIX)) {
			return toLockPage(templateName);
		}
		if (templateName.startsWith(ERRORPAGE_PREFIX)) {
			return toErrorPage(templateName);
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
		return templateName.startsWith("Page:");
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
	 * 从fragment模板名中获取fragment名
	 * 
	 * @param templateName
	 *            模板名
	 * @return
	 */
	public static String getFragmentName(String templateName) {
		return templateName.split(SPLITER)[1];
	}

	/**
	 * 根据PageType来确定页面类型，从而获取templateName
	 * 
	 * @param page
	 * @return
	 */
	public static String getTemplateName(Page page) {
		if (page.getType() == null) {
			return "Page:";
		}
		switch (page.getType()) {
		case ERROR:
			return getTemplateName((ErrorPage) page);
		case LOCK:
			return getTemplateName((LockPage) page);
		case SYSTEM:
			return getTemplateName((SysPage) page);
		case USER:
			return getTemplateName((UserPage) page);
		default:
			throw new SystemException("无法确定" + page.getType() + "的页面类型");
		}
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
	 * 获取错误页面模板名
	 * 
	 * @param errorPage
	 *            错误页面
	 * @return
	 */
	public static String getTemplateName(ErrorPage errorPage) {
		StringBuilder sb = new StringBuilder();
		sb.append(ERRORPAGE_PREFIX).append(errorPage.getErrorCode().name());
		Space space = errorPage.getSpace();
		if (space != null && space.hasId()) {
			sb.append(SPLITER).append(space.getId());
		}
		return sb.toString();
	}

	/**
	 * 获取模板片段的模板名
	 * 
	 * @param fragment
	 *            模板片段
	 * @return
	 */
	public static String getTemplateName(Fragment fragment) {
		if (fragment instanceof UserFragment) {
			return getTemplateName((UserFragment) fragment);
		}
		return FRAGMENT_PREFIX + fragment.getName();
	}

	private static String getTemplateName(UserFragment userFragment) {
		StringBuilder sb = new StringBuilder();
		sb.append(FRAGMENT_PREFIX).append(userFragment.getName()).append(SPLITER).append(userFragment.isGlobal());
		Space space = userFragment.getSpace();
		if (space != null && space.hasId()) {
			sb.append(SPLITER).append(space.getId());
		}
		return sb.toString();
	}

	private static LockPage toLockPage(String templateName) {
		String[] array = templateName.split(SPLITER);
		if (array.length == 2) {
			return new LockPage(array[1]);
		}
		if (array.length == 3) {
			return new LockPage(new Space(Integer.parseInt(array[2])), array[1]);
		}
		throw new SystemException(templateName + "无法转化为解锁页面");
	}

	private static UserPage toUserPage(String templateName) {
		String[] array = templateName.split(SPLITER);
		if (array.length == 2) {
			return new UserPage(array[1]);
		}
		if (array.length == 3) {
			return new UserPage(new Space(Integer.parseInt(array[2])), cleanUserPageAlias(array[1]));
		}
		throw new SystemException(templateName + "无法转化为用户自定义页面");
	}

	private static SysPage toSysPage(String templateName) {
		String[] array = templateName.split(SPLITER);
		if (array.length == 2) {
			return new SysPage(PageTarget.valueOf(array[1]));
		}
		if (array.length == 3) {
			return new SysPage(new Space(Integer.parseInt(array[2])), PageTarget.valueOf(array[1]));
		}
		throw new SystemException(templateName + "无法转化为系统页面");
	}

	private static ErrorPage toErrorPage(String templateName) {
		String[] array = templateName.split(SPLITER);
		if (array.length == 2) {
			return new ErrorPage(ErrorCode.valueOf(array[1]));
		}
		if (array.length == 3) {
			return new ErrorPage(new Space(Integer.parseInt(array[2])), ErrorCode.valueOf(array[1]));
		}
		throw new SystemException(templateName + "无法转化为错误页面");
	}

	/**
	 * 复制一个页面
	 * 
	 * @param page
	 * @return
	 */
	public static Page clone(Page page) {
		if (page.getType() == null) {
			return new Page(page);
		}
		switch (page.getType()) {
		case ERROR:
			return new ErrorPage((ErrorPage) page);
		case LOCK:
			return new LockPage((LockPage) page);
		case SYSTEM:
			return new SysPage((SysPage) page);
		case USER:
			return new UserPage((UserPage) page);
		default:
			throw new SystemException("无法确定" + page.getType() + "的页面类型");
		}
	}

	/**
	 * 复制一个fragment
	 * 
	 * @param fragment
	 * @return
	 */
	public static Fragment clone(Fragment fragment) {
		if (fragment instanceof UserFragment) {
			return new UserFragment((UserFragment) fragment);
		}
		return new Fragment(fragment);
	}

	/**
	 * 构建一个fragment标签
	 * 
	 * @param name
	 *            name属性
	 * @param atts
	 *            其他属性
	 * @return
	 */
	public static String buildFragmentTag(String name, Map<String, String> atts) {
		return buildTag("fragment", name, atts);
	}

	/**
	 * 构建一个data标签
	 * 
	 * @param name
	 *            name属性
	 * @param atts
	 *            其他属性
	 * @return
	 */
	public static String buildDataTag(String name, Map<String, String> atts) {
		return buildTag("data", name, atts);
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

	private static String buildTag(String tagName, String name, Map<String, String> atts) {
		Tag tag = Tag.valueOf(tagName);
		Attributes attributes = new Attributes();
		if (!CollectionUtils.isEmpty(atts)) {
			for (Map.Entry<String, String> it : atts.entrySet()) {
				attributes.put(it.getKey(), it.getValue());
			}
		}
		attributes.put("name", name);
		Element ele = new Element(tag, "", attributes);
		return ele.toString();
	}
}
