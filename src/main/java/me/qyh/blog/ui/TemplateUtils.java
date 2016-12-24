package me.qyh.blog.ui;

import me.qyh.blog.entity.Space;
import me.qyh.blog.exception.SystemException;
import me.qyh.blog.ui.fragment.Fragment;
import me.qyh.blog.ui.fragment.UserFragment;
import me.qyh.blog.ui.page.DisposiblePage;
import me.qyh.blog.ui.page.ErrorPage;
import me.qyh.blog.ui.page.ErrorPage.ErrorCode;
import me.qyh.blog.ui.page.LockPage;
import me.qyh.blog.ui.page.Page;
import me.qyh.blog.ui.page.SysPage;
import me.qyh.blog.ui.page.SysPage.PageTarget;
import me.qyh.blog.ui.page.UserPage;;

public final class TemplateUtils {

	private static final String SPLITER = "%";
	private static final String SYSPAGE_PREFIX = "Page:Sys" + SPLITER;
	private static final String USERPAGE_PREFIX = "Page:User" + SPLITER;
	private static final String LOCKPAGE_PREFIX = "Page:Lock" + SPLITER;
	private static final String ERRORPAGE_PREFIX = "Page:Error" + SPLITER;

	private static final String DISPOSIBLEPAGE_NAME = "Page:Disposable";

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
	 * 判断是否是一次性页面
	 * 
	 * @param templateName
	 * @return
	 */
	public static boolean isDisposablePageTemplate(String templateName) {
		return DISPOSIBLEPAGE_NAME.equals(templateName);
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
			throw new SystemException("必须指定具体的页面类型");
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
		case DISPOSIBLE:
			return DISPOSIBLEPAGE_NAME;
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
		sb.append(USERPAGE_PREFIX).append(userPage.getAlias());
		Space space = userPage.getSpace();
		if (space != null) {
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
		if (space != null) {
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
		if (space != null) {
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
		if (space != null) {
			sb.append(SPLITER).append(space.getId());
		}
		return sb.toString();
	}

	/**
	 * 获取一次性页面的模板名
	 * 
	 * @param page
	 *            页面
	 * @return
	 */
	public static String getTemplateName() {
		return "Page:";
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
		if (space != null) {
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
			return new UserPage(new Space(Integer.parseInt(array[2])), array[1]);
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
			throw new SystemException("必须指定具体的页面类型");
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
		case DISPOSIBLE:
			return new DisposiblePage(page);
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
}
