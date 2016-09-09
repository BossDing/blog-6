package me.qyh.blog.service;

import java.util.List;

import me.qyh.blog.entity.Space;
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.pageparam.PageResult;
import me.qyh.blog.pageparam.UserPageQueryParam;
import me.qyh.blog.pageparam.UserWidgetQueryParam;
import me.qyh.blog.ui.Params;
import me.qyh.blog.ui.page.ErrorPage;
import me.qyh.blog.ui.page.ErrorPage.ErrorCode;
import me.qyh.blog.ui.page.ExpandedPage;
import me.qyh.blog.ui.page.Page;
import me.qyh.blog.ui.page.SysPage;
import me.qyh.blog.ui.page.SysPage.PageTarget;
import me.qyh.blog.ui.page.UserPage;
import me.qyh.blog.ui.widget.UserWidget;
import me.qyh.blog.ui.widget.Widget;
import me.qyh.blog.ui.widget.WidgetTpl;

public interface UIService {

	/**
	 * 插入用户自定义挂件
	 * 
	 * @param userWidget
	 * @throws LogicException
	 */
	void insertUserWidget(UserWidget userWidget) throws LogicException;

	/**
	 * 删除用户自定义挂件
	 * 
	 * @param id
	 * @throws LogicException
	 */
	void deleteUserWidget(Integer id) throws LogicException;

	/**
	 * 分页查询用户自定义挂件
	 * 
	 * @param param
	 * @return
	 */
	PageResult<UserWidget> queryUserWidget(UserWidgetQueryParam param);

	/**
	 * 更新自定义挂件
	 * 
	 * @param userWidget
	 */
	void updateUserWidget(UserWidget userWidget) throws LogicException;

	/**
	 * 根据ID查询用户挂件
	 * 
	 * @param id
	 *            挂件ID
	 * @return null如果不存在
	 */
	UserWidget queryUserWidget(Integer id);

	/**
	 * 根据ID查询用户页面
	 * 
	 * @param id
	 * @return
	 */
	UserPage queryUserPage(Integer id);

	/**
	 * 根据alias查询用户页面
	 * 
	 * @param id
	 * @return
	 */
	UserPage queryUserPage(String alias);

	/**
	 * 查询系统页面模板
	 * 
	 * @param space
	 * @param target
	 * @return
	 */
	SysPage querySysPage(Space space, PageTarget target);

	/**
	 * 分页查询用户自定义页面
	 * 
	 * @param param
	 * @return
	 */
	PageResult<UserPage> queryUserPage(UserPageQueryParam param);

	/**
	 * 删除用户自定义页面
	 * 
	 * @param id
	 * @throws LogicException
	 */
	void deleteUserPage(Integer id) throws LogicException;

	/**
	 * 从系统 页面模板中解析挂件
	 * 
	 * @param page
	 * @return
	 * @throws LogicException
	 */
	List<WidgetTpl> parseWidget(SysPage page) throws LogicException;

	/**
	 * 从自定义页面模板中解析挂件
	 * 
	 * @param page
	 * @return
	 * @throws LogicException
	 */
	List<WidgetTpl> parseWidget(UserPage page) throws LogicException;

	/**
	 * 渲染预览页面
	 * 
	 * @param sysPage
	 * @return
	 * @throws LogicException
	 */
	void renderPreviewPage(SysPage sysPage) throws LogicException;

	/**
	 * 渲染预览页面
	 * 
	 * @param space
	 * @param target
	 * @return
	 * @throws LogicException
	 */
	SysPage renderPreviewPage(Space space, PageTarget target) throws LogicException;

	/**
	 * 渲染预览页面
	 * 
	 * @param userPage
	 * @return
	 * @throws LogicException
	 */
	void renderPreviewPage(UserPage userPage) throws LogicException;

	/**
	 * 保存页面模板
	 * 
	 * @param sysPage
	 * @throws LogicException
	 */
	void buildTpl(SysPage sysPage) throws LogicException;

	/**
	 * 保存页面模板
	 * 
	 * @param userPage
	 * @throws LogicException
	 */
	void buildTpl(UserPage userPage) throws LogicException;

	/**
	 * 渲染页面
	 * 
	 * @param space
	 * @param pageTarget
	 * @param params
	 * @return
	 * @throws LogicException
	 */
	SysPage renderSysPage(Space space, PageTarget pageTarget, Params params) throws LogicException;

	/**
	 * 渲染用户自定义页面
	 * 
	 * @param id
	 * @return 不会为null
	 * @throws LogicException
	 *             如果页面不存在，数据渲染异常等
	 */
	UserPage renderUserPage(String idOrAlias) throws LogicException;

	/**
	 * 删除挂件模板
	 * 
	 * @param page
	 * @param widget
	 * @throws LogicException
	 */
	void deleteWidgetTpl(Page page, Widget widget);

	/**
	 * 删除系统挂件模板
	 * 
	 * @param space
	 * @param target
	 * @throws LogicException
	 */
	void deleteSysPage(Space space, PageTarget target) throws LogicException;

	/**
	 * 渲染拓展页面
	 * 
	 * @param expandedPage
	 * @param params
	 * @return
	 * @throws LogicException
	 */
	ExpandedPage renderExpandedPage(Integer id, Params params) throws LogicException;

	/**
	 * 查询所有的拓展页面
	 * 
	 * @return
	 */
	List<ExpandedPage> queryExpandedPage();

	/**
	 * 还原用户拓展页面
	 * 
	 * @param id
	 * @throws LogicException
	 */
	void deleteExpandedPage(Integer id) throws LogicException;

	/**
	 * 查询用户拓展页面
	 * 
	 * @param id
	 * @return
	 * @throws LogicException
	 */
	ExpandedPage queryExpandedPage(Integer id) throws LogicException;

	/**
	 * 保存/更新拓展页面模板
	 * 
	 * @param page
	 * @throws LogicException
	 */
	void buildTpl(ExpandedPage page) throws LogicException;

	/**
	 * 渲染预览页面
	 * 
	 * @param sysPage
	 * @return
	 * @throws LogicException
	 */
	void renderPreviewPage(ExpandedPage expandedPage) throws LogicException;

	/**
	 * 从自定义页面中解析挂件模板
	 * 
	 * @param page
	 * @return
	 * @throws LogicException
	 */
	List<WidgetTpl> parseWidget(ExpandedPage page) throws LogicException;

	/**
	 * 保存更新错误页面模板
	 * 
	 * @param errorPage
	 * @throws LogicException
	 */
	void buildTpl(ErrorPage errorPage) throws LogicException;

	/**
	 * 删除错误挂件模板
	 * 
	 * @param space
	 * @param target
	 * @throws LogicException
	 */
	void deleteErrorPage(Space space, ErrorCode erroCode) throws LogicException;

	/**
	 * 渲染预览错误页面
	 * 
	 * @param errorPage
	 * @throws LogicException
	 */
	void renderPreviewPage(ErrorPage errorPage) throws LogicException;

	/**
	 * 解析错误页面中的挂件模板
	 * 
	 * @param page
	 * @return
	 * @throws LogicException
	 */
	List<WidgetTpl> parseWidget(ErrorPage page) throws LogicException;

	/**
	 * 查询错误页面
	 * 
	 * @param space
	 * @param code
	 * @return
	 */
	ErrorPage queryErrorPage(Space space, ErrorCode code);

	/**
	 * 渲染错误页面
	 * 
	 * @param space
	 * @param code
	 * @return
	 * @throws LogicException
	 */
	ErrorPage renderErrorPage(Space space, ErrorCode code) throws LogicException;

}
