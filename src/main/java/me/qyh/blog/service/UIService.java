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
package me.qyh.blog.service;

import java.util.List;

import me.qyh.blog.bean.ExportPage;
import me.qyh.blog.bean.ImportOption;
import me.qyh.blog.bean.ImportRecord;
import me.qyh.blog.entity.Space;
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.pageparam.PageResult;
import me.qyh.blog.pageparam.UserFragmentQueryParam;
import me.qyh.blog.pageparam.UserPageQueryParam;
import me.qyh.blog.ui.ContextVariables;
import me.qyh.blog.ui.DataTag;
import me.qyh.blog.ui.data.DataBind;
import me.qyh.blog.ui.fragment.Fragment;
import me.qyh.blog.ui.fragment.UserFragment;
import me.qyh.blog.ui.page.ErrorPage;
import me.qyh.blog.ui.page.ErrorPage.ErrorCode;
import me.qyh.blog.ui.page.LockPage;
import me.qyh.blog.ui.page.Page;
import me.qyh.blog.ui.page.SysPage;
import me.qyh.blog.ui.page.SysPage.PageTarget;
import me.qyh.blog.ui.page.UserPage;

/**
 * 
 * @author Administrator
 *
 */
public interface UIService {

	/**
	 * 插入用户自定义模板片段
	 * 
	 * @param userFragment
	 *            用户自定义模板片段
	 * @throws LogicException
	 */
	void insertUserFragment(UserFragment userFragment) throws LogicException;

	/**
	 * 删除用户自定义挂件
	 * 
	 * @param id
	 *            挂件id
	 * @throws LogicException
	 */
	void deleteUserFragment(Integer id) throws LogicException;

	/**
	 * 分页查询用户自定义模板片段
	 * 
	 * @param param
	 *            查询参数
	 * @return 模板片段分页
	 */
	PageResult<UserFragment> queryUserFragment(UserFragmentQueryParam param);

	/**
	 * 更新自定义挂件
	 * 
	 * @param userFragment
	 */
	void updateUserFragment(UserFragment userFragment) throws LogicException;

	/**
	 * 根据ID查询用户挂件
	 * 
	 * @param id
	 *            挂件ID
	 * @return null如果不存在
	 */
	UserFragment queryUserFragment(Integer id);

	/**
	 * 根据ID查询用户页面
	 * 
	 * @param id
	 * @return
	 */
	UserPage queryUserPage(Integer id);

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
	 * 删除系统挂件模板
	 * 
	 * @param space
	 * @param target
	 * @throws LogicException
	 */
	void deleteSysPage(Space space, PageTarget target) throws LogicException;

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
	 * @param errorCode
	 * @throws LogicException
	 */
	void deleteErrorPage(Space space, ErrorCode errorCode) throws LogicException;

	/**
	 * 通过DATA_TAG标签查询数据
	 * 
	 * @param dataTagStr
	 * @return
	 * @throws LogicException
	 */
	DataBind<?> queryData(DataTag dataTag, ContextVariables variables) throws LogicException;

	/**
	 * 通过DATA_TAG标签查询预览数据
	 * 
	 * @param dataTagStr
	 * @return
	 * @throws LogicException
	 */
	DataBind<?> queryPreviewData(DataTag dataTag);

	/**
	 * 查询系统数据
	 * 
	 * @return
	 */
	List<String> queryDataTags();

	/**
	 * 查询系统模板片段
	 * 
	 * @return
	 */
	List<Fragment> querySysFragments();

	/**
	 * 根据name查询fragment
	 * 
	 * @param name
	 * @return
	 */
	Fragment queryFragment(String name);

	/**
	 * 创建解锁页面模板
	 * 
	 * @param lockPage
	 * @throws LogicException
	 */
	void buildTpl(LockPage lockPage) throws LogicException;

	/**
	 * 删除存在的解锁页面模板
	 * 
	 * @param space
	 * @param lockType
	 * @throws LogicException
	 */
	void deleteLockPage(Space space, String lockType) throws LogicException;

	/**
	 * 根据模板名查询页面
	 * 
	 * @param templateName
	 *            模板页面
	 * @return 不会为null
	 * @throws LogicException
	 *             当空间或者页面不存在时
	 */
	Page queryPage(String templateName) throws LogicException;

	/**
	 * 根据空间导出页面
	 * 
	 * @param space
	 *            空间
	 * @return
	 * @throws LogicException
	 */
	List<ExportPage> exportPage(Space space) throws LogicException;

	/**
	 * 导出单个页面
	 * 
	 * @param templateName
	 * @return
	 * @throws LogicException
	 */
	ExportPage exportPage(String templateName) throws LogicException;

	/**
	 * 导入模板
	 * 
	 * @param space
	 *            空间
	 * @param exportPages
	 *            要导入的页面
	 * @param importOption
	 *            操作选择
	 */
	List<ImportRecord> importPage(Space space, List<ExportPage> exportPages, ImportOption importOption);

}
