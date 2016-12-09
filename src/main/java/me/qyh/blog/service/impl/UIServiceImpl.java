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
package me.qyh.blog.service.impl;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.CharStreams;

import me.qyh.blog.bean.ExportReq;
import me.qyh.blog.bean.ImportError;
import me.qyh.blog.bean.ImportPageWrapper;
import me.qyh.blog.bean.ImportReq;
import me.qyh.blog.bean.ImportResult;
import me.qyh.blog.bean.ImportSuccess;
import me.qyh.blog.config.Constants;
import me.qyh.blog.dao.ErrorPageDao;
import me.qyh.blog.dao.ExpandedPageDao;
import me.qyh.blog.dao.LockPageDao;
import me.qyh.blog.dao.SysPageDao;
import me.qyh.blog.dao.UserFragmentDao;
import me.qyh.blog.dao.UserPageDao;
import me.qyh.blog.entity.Space;
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.exception.SystemException;
import me.qyh.blog.lock.LockManager;
import me.qyh.blog.message.Message;
import me.qyh.blog.pageparam.PageResult;
import me.qyh.blog.pageparam.UserFragmentQueryParam;
import me.qyh.blog.pageparam.UserPageQueryParam;
import me.qyh.blog.service.ConfigService;
import me.qyh.blog.service.UIService;
import me.qyh.blog.ui.DataTag;
import me.qyh.blog.ui.ExportPage;
import me.qyh.blog.ui.Params;
import me.qyh.blog.ui.ParseResult;
import me.qyh.blog.ui.RenderedPage;
import me.qyh.blog.ui.TemplateParser;
import me.qyh.blog.ui.TemplateParser.DataQuery;
import me.qyh.blog.ui.TemplateParser.FragmentQuery;
import me.qyh.blog.ui.data.DataBind;
import me.qyh.blog.ui.data.DataTagProcessor;
import me.qyh.blog.ui.fragment.Fragment;
import me.qyh.blog.ui.fragment.UserFragment;
import me.qyh.blog.ui.page.ErrorPage;
import me.qyh.blog.ui.page.ErrorPage.ErrorCode;
import me.qyh.blog.ui.page.ExpandedPage;
import me.qyh.blog.ui.page.ExpandedPageHandler;
import me.qyh.blog.ui.page.ExpandedPageServer;
import me.qyh.blog.ui.page.LockPage;
import me.qyh.blog.ui.page.Page;
import me.qyh.blog.ui.page.SysPage;
import me.qyh.blog.ui.page.SysPage.PageTarget;
import me.qyh.blog.ui.page.UserPage;
import me.qyh.blog.util.Validators;
import me.qyh.blog.web.controller.form.PageValidator;
import me.qyh.blog.web.interceptor.SpaceContext;

@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
public class UIServiceImpl implements UIService, InitializingBean {

	@Autowired
	private SysPageDao sysPageDao;
	@Autowired
	private UserPageDao userPageDao;
	@Autowired
	private ErrorPageDao errorPageDao;
	@Autowired
	private LockPageDao lockPageDao;
	@Autowired
	private UserFragmentDao userFragmentDao;
	@Autowired
	private SpaceCache spaceCache;
	@Autowired
	private ExpandedPageDao expandedPageDao;
	@Autowired
	private ExpandedPageServer expandedPageServer;
	@Autowired
	private LockManager lockManager;
	@Autowired
	private ConfigService configService;

	private UICacheRender uiCacheRender;

	private Map<PageTarget, Resource> sysPageDefaultTpls = Maps.newEnumMap(PageTarget.class);
	private Map<PageTarget, String> sysPageDefaultParsedTpls = Maps.newEnumMap(PageTarget.class);
	private Map<ErrorCode, Resource> errorPageDefaultTpls = Maps.newEnumMap(ErrorCode.class);
	private Map<ErrorCode, String> errorPageDefaultParsedTpls = Maps.newEnumMap(ErrorCode.class);
	private Map<String, String> lockPageDefaultTpls = Maps.newHashMap();
	private List<DataTagProcessor<?>> processors = Lists.newArrayList();

	private final TemplateParser templateParser = new TemplateParser();

	private final DataQuery previewDataQuery = new DataQuery() {

		@Override
		public DataBind<?> query(DataTag dataTag) throws LogicException {
			DataTagProcessor<?> processor = geTagProcessor(dataTag.getName());
			if (processor != null) {
				return processor.previewData(dataTag.getAttrs());
			}
			return null;
		}
	};

	private static final Message SPACE_NOT_EXISTS = new Message("space.notExists", "空间不存在");
	private static final Message USER_PAGE_NOT_EXISTS = new Message("page.user.notExists", "自定义页面不存在");

	/**
	 * 系统默认片段
	 */
	private List<Fragment> fragments = Lists.newArrayList();

	@Override
	public void insertUserFragment(UserFragment userFragment) throws LogicException {
		Space space = userFragment.getSpace();
		if (space != null && spaceCache.getSpace(space.getId()) == null) {
			throw new LogicException(SPACE_NOT_EXISTS);
		}
		UserFragment db;
		if (userFragment.isGlobal()) {
			db = userFragmentDao.selectGlobalByName(userFragment.getName());
		} else {
			db = userFragmentDao.selectBySpaceAndName(space, userFragment.getName());
		}
		boolean nameExists = db != null;
		if (nameExists) {
			throw new LogicException("fragment.user.nameExists", "挂件名:" + userFragment.getName() + "已经存在",
					userFragment.getName());
		}

		userFragment.setCreateDate(Timestamp.valueOf(LocalDateTime.now()));
		userFragmentDao.insert(userFragment);

		uiCacheRender.evit(userFragment);
	}

	@Override
	public void deleteUserFragment(Integer id) throws LogicException {
		UserFragment userFragment = userFragmentDao.selectById(id);
		if (userFragment == null) {
			throw new LogicException("fragment.user.notExists", "挂件不存在");
		}
		userFragmentDao.deleteById(id);

		uiCacheRender.evit(userFragment);
	}

	@Override
	public void updateUserFragment(UserFragment userFragment) throws LogicException {
		Space space = userFragment.getSpace();
		if (space != null && spaceCache.getSpace(space.getId()) == null) {
			throw new LogicException(SPACE_NOT_EXISTS);
		}
		UserFragment old = userFragmentDao.selectById(userFragment.getId());
		if (old == null) {
			throw new LogicException("fragment.user.notExists", "挂件不存在");
		}
		UserFragment db;
		// 查找当前数据库是否存在同名
		if (userFragment.isGlobal()) {
			db = userFragmentDao.selectGlobalByName(userFragment.getName());
		} else {
			db = userFragmentDao.selectBySpaceAndName(space, userFragment.getName());
		}
		boolean nameExists = db != null && !db.getId().equals(userFragment.getId());
		if (nameExists) {
			throw new LogicException("fragment.user.nameExists", "挂件名:" + userFragment.getName() + "已经存在",
					userFragment.getName());
		}
		userFragmentDao.update(userFragment);

		uiCacheRender.evit(old, userFragment);
	}

	@Override
	@Transactional(readOnly = true)
	public PageResult<UserFragment> queryUserFragment(UserFragmentQueryParam param) {
		param.setPageSize(configService.getGlobalConfig().getUserFragmentPageSize());
		int count = userFragmentDao.selectCount(param);
		List<UserFragment> datas = userFragmentDao.selectPage(param);
		return new PageResult<UserFragment>(param, count, datas);
	}

	@Override
	@Transactional(readOnly = true)
	public UserFragment queryUserFragment(Integer id) {
		return userFragmentDao.selectById(id);
	}

	@Override
	@Transactional(readOnly = true)
	public UserPage queryUserPage(Integer id) {
		return userPageDao.selectById(id);
	}

	@Override
	@Transactional(readOnly = true)
	public UserPage queryUserPage(String alias) {
		return userPageDao.selectBySpaceAndAlias(SpaceContext.get(), alias);
	}

	@Override
	@Transactional(readOnly = true)
	public PageResult<UserPage> queryUserPage(UserPageQueryParam param) {
		param.setPageSize(configService.getGlobalConfig().getUserPagePageSize());
		int count = userPageDao.selectCount(param);
		List<UserPage> datas = userPageDao.selectPage(param);
		return new PageResult<UserPage>(param, count, datas);
	}

	@Override
	public void deleteUserPage(Integer id) throws LogicException {
		UserPage db = userPageDao.selectById(id);
		if (db == null) {
			throw new LogicException(USER_PAGE_NOT_EXISTS);
		}
		userPageDao.deleteById(id);
		uiCacheRender.evit(new UserPageLoader(db.getSpace(), db.getAlias()));
	}

	@Override
	@Transactional(readOnly = true)
	public SysPage querySysPage(Space space, PageTarget target) {
		SysPage sysPage = sysPageDao.selectBySpaceAndPageTarget(space, target);
		if (sysPage == null) {
			sysPage = new SysPage(space, target);
			sysPage.setTpl(sysPageDefaultParsedTpls.get(target));
		}
		sysPage.setSpace(space);
		return sysPage;
	}

	@Override
	public List<String> queryDataTags() {
		List<String> dataTags = Lists.newArrayList();
		for (DataTagProcessor<?> processor : processors) {
			dataTags.add(processor.getName());
		}
		return dataTags;
	}

	@Override
	public List<Fragment> querySysFragments() {
		return fragments;
	}

	@Override
	@Transactional(readOnly = true)
	public RenderedPage renderPreviewPage(final Space space, PageTarget target) throws LogicException {
		Space db = spaceCache.getSpace(space.getId());
		if (db == null) {
			throw new LogicException(SPACE_NOT_EXISTS);
		}
		return uiCacheRender.renderPreview(new SysPageLoader(target, db));
	}

	@Override
	@Transactional(readOnly = true)
	public RenderedPage renderPreviewPage(Page page) throws LogicException {
		ParseResult result = templateParser.parse(page.getTpl(), previewDataQuery,
				new FragmentQueryImpl(page.getSpace()));
		return new RenderedPage(page, result.getBinds(), result.getFragments());
	}

	@Override
	@Transactional(readOnly = true)
	public RenderedPage renderSysPage(final Space space, final PageTarget pageTarget, Params params)
			throws LogicException {
		return uiCacheRender.render(new SysPageLoader(pageTarget, space), params);
	}

	@Override
	@Transactional(readOnly = true)
	public RenderedPage renderUserPage(String alias) throws LogicException {
		return uiCacheRender.render(new UserPageLoader(SpaceContext.get(), alias), new Params());
	}

	@Override
	public void buildTpl(SysPage sysPage) throws LogicException {
		checkSpace(sysPage);
		SysPage db = sysPageDao.selectBySpaceAndPageTarget(sysPage.getSpace(), sysPage.getTarget());
		boolean update = db != null;
		if (update) {
			sysPage.setId(db.getId());
			sysPageDao.update(sysPage);
		} else {
			sysPageDao.insert(sysPage);
		}
		uiCacheRender.evit(new SysPageLoader(sysPage.getTarget(), sysPage.getSpace()));
	}

	@Override
	public void buildTpl(UserPage userPage) throws LogicException {
		checkSpace(userPage);
		String alias = userPage.getAlias();
		userPage.setCreateDate(Timestamp.valueOf(LocalDateTime.now()));
		boolean update = userPage.hasId();
		if (update) {
			UserPage db = userPageDao.selectById(userPage.getId());
			if (db == null) {
				throw new LogicException(USER_PAGE_NOT_EXISTS);
			}
			// 检查
			UserPage aliasPage = userPageDao.selectBySpaceAndAlias(db.getSpace(), alias);
			if (aliasPage != null && !aliasPage.getId().equals(db.getId())) {
				throw new LogicException("page.user.aliasExists", "别名" + alias + "已经存在", alias);
			}
			userPage.setId(db.getId());
			userPageDao.update(userPage);
			uiCacheRender.evit(new UserPageLoader(db.getSpace(), db.getAlias()));
		} else {
			// 检查
			UserPage aliasPage = userPageDao.selectBySpaceAndAlias(userPage.getSpace(), alias);
			if (aliasPage != null)
				throw new LogicException("page.user.aliasExists", "别名" + alias + "已经存在", alias);
			userPageDao.insert(userPage);
		}
	}

	@Override
	public void deleteSysPage(Space space, PageTarget target) throws LogicException {
		SysPage page = sysPageDao.selectBySpaceAndPageTarget(space, target);
		if (page != null) {
			sysPageDao.deleteById(page.getId());
			uiCacheRender.evit(new SysPageLoader(target, space));
		}
	}

	@Override
	@Transactional(readOnly = true)
	public RenderedPage renderExpandedPage(final Integer id, Params params) throws LogicException {
		return uiCacheRender.render(new ExpandedPageLoader(id), params);
	}

	@Override
	@Transactional(readOnly = true)
	public List<ExpandedPage> queryExpandedPage() {
		List<ExpandedPage> pages = Lists.newArrayList();
		if (!expandedPageServer.isEmpty()) {
			List<ExpandedPage> dbs = expandedPageDao.selectAll();
			for (ExpandedPageHandler handler : expandedPageServer.getHandlers()) {
				ExpandedPage page = new ExpandedPage();
				page.setId(handler.id());
				page.setName(handler.name());
				for (ExpandedPage db : dbs) {
					if (page.getId().equals(db.getId())) {
						page.setName(db.getName());
						break;
					}
				}
				pages.add(page);
			}
		}
		return pages;
	}

	@Override
	public void deleteExpandedPage(Integer id) throws LogicException {
		ExpandedPage page = expandedPageDao.selectById(id);
		if (page != null) {
			expandedPageDao.deleteById(id);
			uiCacheRender.evit(new ExpandedPageLoader(id));
		}
	}

	@Override
	@Transactional(readOnly = true)
	public ExpandedPage queryExpandedPage(Integer id) throws LogicException {
		ExpandedPage page = expandedPageDao.selectById(id);
		if (page == null) {
			page = new ExpandedPage();
			ExpandedPageHandler handler = expandedPageServer.get(id);
			if (handler == null) {
				throw new LogicException("page.expanded.notExists", "拓展页面不存在");
			}
			page.setId(id);
			page.setName(handler.name());
			page.setTpl(handler.getTemplate());
		}
		return page;
	}

	@Override
	public void buildTpl(ExpandedPage page) throws LogicException {
		if (!expandedPageServer.hasHandler(page.getId())) {
			throw new LogicException("page.expanded.notExists", "拓展页面不存在");
		}
		ExpandedPage db = expandedPageDao.selectById(page.getId());
		boolean update = db != null;
		if (update) {
			page.setId(db.getId());
			expandedPageDao.update(page);
		} else {
			expandedPageDao.insert(page);
		}
		uiCacheRender.evit(new ExpandedPageLoader(page.getId()));
	}

	@Override
	public void buildTpl(LockPage lockPage) throws LogicException {
		checkLockType(lockPage.getLockType());
		checkSpace(lockPage);
		LockPage db = lockPageDao.selectBySpaceAndLockType(lockPage.getSpace(), lockPage.getLockType());
		boolean update = db != null;
		if (update) {
			lockPage.setId(db.getId());
			lockPageDao.update(lockPage);
		} else {
			lockPageDao.insert(lockPage);
		}
		uiCacheRender.evit(new LockPageLoader(lockPage.getLockType(), lockPage.getSpace()));
	}

	@Override
	public void deleteLockPage(Space space, String lockType) throws LogicException {
		LockPage page = lockPageDao.selectBySpaceAndLockType(space, lockType);
		if (page != null) {
			lockPageDao.deleteById(page.getId());
			uiCacheRender.evit(new LockPageLoader(lockType, space));
		}
	}

	@Override
	@Transactional(readOnly = true)
	public LockPage queryLockPage(Space space, String lockType) throws LogicException {
		checkLockType(lockType);
		LockPage lockPage = lockPageDao.selectBySpaceAndLockType(space, lockType);
		if (lockPage == null) {
			lockPage = new LockPage(space, lockType);
			lockPage.setTpl(lockPageDefaultTpls.get(lockType));
		}
		lockPage.setSpace(space);
		return lockPage;
	}

	@Override
	@Transactional(readOnly = true)
	public RenderedPage renderLockPage(final Space space, String lockType) throws LogicException {
		checkLockType(lockType);
		return uiCacheRender.render(new LockPageLoader(lockType, space), new Params());
	}

	private void checkLockType(String lockType) throws LogicException {
		if (!lockManager.checkLockTypeExists(lockType)) {
			throw new LogicException("page.lock.locktype.notexists", "锁类型：" + lockType + "不存在", lockType);
		}
	}

	@Override
	public void buildTpl(ErrorPage errorPage) throws LogicException {
		checkSpace(errorPage);
		ErrorPage db = errorPageDao.selectBySpaceAndErrorCode(errorPage.getSpace(), errorPage.getErrorCode());
		boolean update = db != null;
		if (update) {
			errorPage.setId(db.getId());
			errorPageDao.update(errorPage);
		} else {
			errorPageDao.insert(errorPage);
		}
		uiCacheRender.evit(new ErrorPageLoader(errorPage.getErrorCode(), errorPage.getSpace()));
	}

	@Override
	public void deleteErrorPage(Space space, ErrorCode errorCode) throws LogicException {
		ErrorPage page = errorPageDao.selectBySpaceAndErrorCode(space, errorCode);
		if (page != null) {
			errorPageDao.deleteById(page.getId());
			uiCacheRender.evit(new ErrorPageLoader(errorCode, space));
		}
	}

	@Override
	@Transactional(readOnly = true)
	public ErrorPage queryErrorPage(Space space, ErrorCode code) {
		ErrorPage db = errorPageDao.selectBySpaceAndErrorCode(space, code);
		if (db == null) {
			db = new ErrorPage(space, code);
			db.setTpl(errorPageDefaultParsedTpls.get(code));
		}
		return db;
	}

	@Override
	@Transactional(readOnly = true)
	public RenderedPage renderErrorPage(final Space space, ErrorCode code) throws LogicException {
		return uiCacheRender.render(new ErrorPageLoader(code, space), new Params());
	}

	@Override
	@Transactional(readOnly = true)
	public List<ExportPage> export(ExportReq req) throws LogicException {
		Space space = req.getSpace();
		final Space sp = space == null ? null : spaceCache.getSpace(space.getId());
		if (space != null && sp == null) {
			throw new LogicException(SPACE_NOT_EXISTS);
		}
		List<ExportPage> pages = Lists.newArrayList();
		// 系统页面
		for (PageTarget target : PageTarget.values()) {
			RenderedPage page = uiCacheRender.renderPreview(new SysPageLoader(target, sp));
			ExportPage ep = new ExportPage();
			ep.setPage(new SysPage(null, target));
			ep.getPage().setTpl(page.getPage().getTpl());
			ep.setFragments(Lists.newArrayList(page.getFragmentMap().values()));
			pages.add(ep);
		}
		// 错误页面
		for (ErrorCode errorCode : ErrorCode.values()) {
			RenderedPage page = uiCacheRender.renderPreview(new ErrorPageLoader(errorCode, sp));
			ExportPage ep = new ExportPage();
			ep.setPage(new ErrorPage(null, errorCode));
			ep.getPage().setTpl(page.getPage().getTpl());
			ep.setFragments(Lists.newArrayList(page.getFragmentMap().values()));
			pages.add(ep);
		}
		// 个人页面
		for (UserPage up : userPageDao.selectBySpace(sp)) {
			RenderedPage page = uiCacheRender.render(up);
			ExportPage ep = new ExportPage();
			ep.setPage(up);
			ep.getPage().setTpl(page.getPage().getTpl());
			ep.setFragments(Lists.newArrayList(page.getFragmentMap().values()));
			pages.add(ep);
		}
		// 解锁
		for (String lockType : lockManager.allTypes()) {
			RenderedPage page = uiCacheRender.renderPreview(new LockPageLoader(lockType, sp));
			ExportPage ep = new ExportPage();
			ep.setPage(new LockPage(null, lockType));
			ep.getPage().setTpl(page.getPage().getTpl());
			ep.setFragments(Lists.newArrayList(page.getFragmentMap().values()));
			pages.add(ep);
		}
		if (req.isExportExpandedPage()) {
			for (ExpandedPage ep : expandedPageDao.selectAll()) {
				RenderedPage page = uiCacheRender.renderPreview(new ExpandedPageLoader(ep.getId()));
				ExportPage ep2 = new ExportPage();
				ep2.setPage(new ExpandedPage(ep.getId()));
				ep2.getPage().setTpl(page.getPage().getTpl());
				ep2.setFragments(Lists.newArrayList(page.getFragmentMap().values()));
				pages.add(ep2);
			}
		}
		return pages;
	}

	@Override
	public ImportResult importTemplate(List<ImportPageWrapper> wrappers, ImportReq req) throws LogicException {
		Space space = req.getSpace();
		if (space != null) {
			space = spaceCache.getSpace(space.getId());
			if (space == null)
				throw new LogicException(SPACE_NOT_EXISTS);
		}
		ImportResult result = new ImportResult();
		for (ImportPageWrapper ipw : wrappers) {
			ExportPage ep = ipw.getPage();
			Page page = ep.getPage();

			Page db = null;
			switch (ep.getPage().getType()) {
			case USER:
				db = userPageDao.selectBySpaceAndAlias(req.getSpace(), ((UserPage) page).getAlias());
				break;
			case SYSTEM:
				db = querySysPage(space, ((SysPage) page).getTarget());
				break;
			case ERROR:
				db = queryErrorPage(space, ((ErrorPage) page).getErrorCode());
				break;
			case EXPANDED:
				db = expandedPageDao.selectById(page.getId());
				if (db == null) {
					db = new ExpandedPage();
					ExpandedPageHandler handler = expandedPageServer.get(page.getId());
					if (handler == null) {
						continue;
					}
					db.setId(page.getId());
					((ExpandedPage) db).setName(handler.name());
					db.setTpl(handler.getTemplate());
					expandedPageDao.insert((ExpandedPage) db);
				}
				break;
			case LOCK:
				try {
					db = queryLockPage(space, ((LockPage) page).getLockType());
				} catch (LogicException e) {
					result.addError(new ImportError(ipw.getIndex(), e.getLogicMessage()));
					continue;
				}
				break;
			}
			// 如果以前页面不存在了，直接跳过
			if (db == null) {
				String param = null;
				switch (page.getType()) {
				case USER:
					param = ((UserPage) page).getAlias();
					break;
				case EXPANDED:
					param = page.getId() + "";
					break;
				case SYSTEM:
					param = ((SysPage) page).getTarget().name();
					break;
				case ERROR:
					param = ((ErrorPage) page).getErrorCode().name();
					break;
				case LOCK:
					param = ((LockPage) page).getLockType();
					break;
				}
				result.addError(new ImportError(ipw.getIndex(), new Message("tpl.import.pageNotExists",
						"页面[" + page.getType() + "," + param + "]不存在", page.getType(), param)));
				continue;
			}
			ImportSuccess success = new ImportSuccess(ipw.getIndex());
			// 渲染以前的页面模板用于保存
			RenderedPage old = uiCacheRender.render(db);
			result.addOldPage(new ExportPage(old.getPage(), Lists.newArrayList(old.getFragmentMap().values())));
			// 更新页面模板
			db.setTpl(page.getTpl());
			switch (page.getType()) {
			case USER:
				userPageDao.update((UserPage) db);
				break;
			case SYSTEM:
				// 系统模板可能从来没有被覆盖过，所以这里需要再次检查
				if (db.hasId()) {
					sysPageDao.update((SysPage) db);
				} else {
					sysPageDao.insert((SysPage) db);
				}
				break;
			case ERROR:
				if (db.hasId()) {
					errorPageDao.update((ErrorPage) db);
				} else {
					errorPageDao.insert((ErrorPage) db);
				}
				break;
			case EXPANDED:
				expandedPageDao.update((ExpandedPage) db);
				break;
			case LOCK:
				// 系统模板可能从来没有被覆盖过，所以这里需要再次检查
				if (db.hasId()) {
					lockPageDao.update((LockPage) db);
				} else {
					lockPageDao.insert((LockPage) db);
				}
				break;
			}

			List<Fragment> fragments = ep.getFragments();
			if (!CollectionUtils.isEmpty(fragments)) {
				label1: for (Fragment fragment : fragments) {
					UserFragment uf = userFragmentDao.selectBySpaceAndName(space, fragment.getName());
					if (uf != null) {
						if (req.isUpdateExistsFragment()) {
							uf.setTpl(fragment.getTpl());
							userFragmentDao.update(uf);
						}
					} else {
						// 查找全局挂件
						for (Fragment glf : this.fragments) {
							if (glf.equals(fragment)) {
								if (glf.getTpl().equals(fragment.getTpl())) {
									continue label1;
								}
							}
						}
						if (req.isInsertNotExistsFragment()) {
							UserFragment newF = new UserFragment();
							newF.setCreateDate(Timestamp.valueOf(LocalDateTime.now()));
							newF.setDescription("");
							newF.setName(fragment.getName());
							newF.setTpl(fragment.getTpl());
							newF.setSpace(space);
							userFragmentDao.insert(newF);
						}
					}
				}
			}

			result.addSuccess(success);
		}
		uiCacheRender.clear();
		return result;
	}

	@Override
	@Transactional(readOnly = true)
	public DataBind<?> queryData(DataTag dataTag) throws LogicException {
		DataTagProcessor<?> processor = geTagProcessor(dataTag.getName());
		if (processor != null) {
			return processor.getData(SpaceContext.get(), new Params(), dataTag.getAttrs());
		}
		return null;
	}

	@Override
	@Transactional(readOnly = true)
	public Fragment queryFragment(String name) {
		return new FragmentQueryImpl(SpaceContext.get()).query(name);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		for (PageTarget target : PageTarget.values()) {
			Resource resource = sysPageDefaultTpls.get(target);
			if (resource == null) {
				resource = new ClassPathResource("resources/page/PAGE_" + target.name() + ".html");
			}
			String tpl = fromResource(resource);
			if (tpl.length() > PageValidator.PAGE_TPL_MAX_LENGTH) {
				throw new SystemException("系统页面：" + target + "模板不能超过" + PageValidator.PAGE_TPL_MAX_LENGTH + "个字符");
			}
			if (Validators.isEmptyOrNull(tpl, true)) {
				throw new SystemException("系统页面：" + target + "模板不能为空");
			}
			sysPageDefaultParsedTpls.put(target, tpl);
		}
		for (ErrorCode code : ErrorCode.values()) {
			Resource resource = errorPageDefaultTpls.get(code);
			if (resource == null) {
				resource = new ClassPathResource("resources/page/" + code.name() + ".html");
			}
			String tpl = fromResource(resource);
			if (tpl.length() > PageValidator.PAGE_TPL_MAX_LENGTH) {
				throw new SystemException("错误页面：" + code + "模板不能超过" + PageValidator.PAGE_TPL_MAX_LENGTH + "个字符");
			}
			if (Validators.isEmptyOrNull(tpl, true)) {
				throw new SystemException("错误页面：" + code + "模板不能为空");
			}
			errorPageDefaultParsedTpls.put(code, tpl);
		}
		for (String lockType : lockManager.allTypes()) {
			Resource resource = lockManager.getDefaultTemplateResource(lockType);
			if (resource == null) {
				throw new SystemException("没有指定LockType:" + lockType + "的默认模板");
			}
			String tpl = fromResource(resource);
			if (tpl.length() > PageValidator.PAGE_TPL_MAX_LENGTH) {
				throw new SystemException("解锁页面：" + lockType + "模板不能超过" + PageValidator.PAGE_TPL_MAX_LENGTH + "个字符");
			}
			if (Validators.isEmptyOrNull(tpl, true)) {
				throw new SystemException("解锁页面：" + lockType + "模板不能为空");
			}
			lockPageDefaultTpls.put(lockType, tpl);
		}
		this.uiCacheRender = new UICacheRender();
	}

	private String fromResource(Resource resource) {
		try (InputStream is = resource.getInputStream();
				InputStreamReader ir = new InputStreamReader(is, Constants.CHARSET)) {
			return CharStreams.toString(ir);
		} catch (Exception e) {
			throw new SystemException(e.getMessage(), e);
		}
	}

	public void setErrorPageDefaultTpls(Map<ErrorCode, Resource> errorPageDefaultTpls) {
		this.errorPageDefaultTpls = errorPageDefaultTpls;
	}

	public void setSysPageDefaultTpls(Map<PageTarget, Resource> sysPageDefaultTpls) {
		this.sysPageDefaultTpls = sysPageDefaultTpls;
	}

	private DataTagProcessor<?> geTagProcessor(String name) {
		for (DataTagProcessor<?> processor : processors) {
			if (processor.getName().equals(name)) {
				return processor;
			}
		}
		return null;
	}

	private void checkSpace(Page page) throws LogicException {
		Space space = page.getSpace();
		if (space != null) {
			space = spaceCache.getSpace(space.getId());
			if (space == null) {
				throw new LogicException(SPACE_NOT_EXISTS);
			}
			page.setSpace(space);
		}
	}

	private final class UICacheRender {

		private final LoadingCache<PageLoader, ParseResultWrapper> cache = CacheBuilder.newBuilder()
				.build(new CacheLoader<PageLoader, ParseResultWrapper>() {

					@Override
					public ParseResultWrapper load(PageLoader loader) throws Exception {
						final Page db = loader.loadFromDb();
						ParseResult parseResult = templateParser.parse(db.getTpl(), noDataQuery,
								new FragmentQueryImpl(db.getSpace()));
						return new ParseResultWrapper(parseResult, db);
					}

				});

		private ParseResultWrapper get(PageLoader loader) throws LogicException {
			try {
				return cache.get(loader);
			} catch (ExecutionException e) {
				Throwable cause = e.getCause();
				if (cause instanceof LogicException) {
					throw (LogicException) cause;
				}
				throw new SystemException(e.getMessage(), e);
			}
		}

		public RenderedPage render(Page db) throws LogicException {
			ParseResult result = templateParser.parse(db.getTpl(), noDataQuery, new FragmentQueryImpl(db.getSpace()));
			List<DataBind<?>> binds = Lists.newArrayList();
			for (DataTag unkownData : result.getUnkownDatas()) {
				//
				DataTagProcessor<?> processor = geTagProcessor(unkownData.getName());
				if (processor != null) {
					binds.add(processor.previewData(unkownData.getAttrs()));
				}
			}
			return new RenderedPage(db, binds, result.getFragments());
		}

		public RenderedPage renderPreview(PageLoader loader) throws LogicException {
			ParseResultWrapper cached = get(loader);
			ParseResult result = cached.parseResult;
			List<DataBind<?>> binds = Lists.newArrayList();
			for (DataTag unkownData : result.getUnkownDatas()) {
				//
				DataTagProcessor<?> processor = geTagProcessor(unkownData.getName());
				if (processor != null) {
					binds.add(processor.previewData(unkownData.getAttrs()));
				}
			}
			return new RenderedPage((Page) cached.page.clone(), binds, result.getFragments());
		}

		public RenderedPage render(PageLoader loader, Params params) throws LogicException {
			ParseResultWrapper cached = get(loader);
			ParseResult result = cached.parseResult;
			List<DataBind<?>> binds = Lists.newArrayList();
			for (DataTag unkownData : result.getUnkownDatas()) {
				//
				DataTagProcessor<?> processor = geTagProcessor(unkownData.getName());
				if (processor != null) {
					binds.add(processor.getData(cached.page.getSpace(), params, unkownData.getAttrs()));
				}
			}
			return new RenderedPage((Page) cached.page.clone(), binds, result.getFragments());
		}

		public void evit(PageLoader key) {
			cache.invalidate(key);
		}

		public void clear() {
			cache.invalidateAll();
		}

		public void evit(Fragment... fragments) {
			for (Map.Entry<PageLoader, ParseResultWrapper> it : cache.asMap().entrySet()) {
				ParseResultWrapper st = it.getValue();
				Map<String, Fragment> currentFMap = st.parseResult.getFragments();
				labe1: for (Fragment currentF : currentFMap.values()) {
					for (Fragment fragment : fragments) {
						if (fragment.getName().equals(currentF.getName())) {
							cache.invalidate(it.getKey());
							break labe1;
						}
					}
				}
				label2: for (String name : st.parseResult.getUnkownFragments()) {
					for (Fragment fragment : fragments) {
						if (name.equals(fragment.getName())) {
							cache.invalidate(it.getKey());
							break label2;
						}
					}
				}
			}
		}

		private final class ParseResultWrapper {
			private ParseResult parseResult;
			private Page page;

			public ParseResultWrapper(ParseResult parseResult, Page page) {
				this.parseResult = parseResult;
				this.page = page;
			}
		}

		private final DataQuery noDataQuery = new DataQuery() {

			@Override
			public DataBind<?> query(DataTag dataTag) throws LogicException {
				return null;
			}
		};
	}

	private interface PageLoader {
		Page loadFromDb() throws LogicException;
	}

	private final class SysPageLoader implements PageLoader {
		private final PageTarget target;
		private final Space space;

		public SysPageLoader(PageTarget target, Space space) {
			super();
			this.target = target;
			this.space = space;
		}

		@Override
		public Page loadFromDb() throws LogicException {
			return querySysPage(space, target);
		}

		@Override
		public int hashCode() {
			return Objects.hash(this.target, this.space);
		}

		@Override
		public boolean equals(Object obj) {
			if (Validators.baseEquals(this, obj)) {
				SysPageLoader other = (SysPageLoader) obj;
				return Objects.equals(this.target, other.target) && Objects.equals(this.space, other.space);
			}
			return false;
		}
	}

	private final class ErrorPageLoader implements PageLoader {
		private final ErrorCode errorCode;
		private final Space space;

		public ErrorPageLoader(ErrorCode errorCode, Space space) {
			this.errorCode = errorCode;
			this.space = space;
		}

		@Override
		public Page loadFromDb() throws LogicException {
			return queryErrorPage(space, errorCode);
		}

		@Override
		public int hashCode() {
			return Objects.hash(this.errorCode, this.space);
		}

		@Override
		public boolean equals(Object obj) {
			if (Validators.baseEquals(this, obj)) {
				ErrorPageLoader other = (ErrorPageLoader) obj;
				return Objects.equals(this.errorCode, other.errorCode) && Objects.equals(this.space, other.space);
			}
			return false;
		}
	}

	private final class LockPageLoader implements PageLoader {
		private final String lockType;
		private final Space space;

		public LockPageLoader(String lockType, Space space) {
			this.lockType = lockType;
			this.space = space;
		}

		@Override
		public Page loadFromDb() throws LogicException {
			return queryLockPage(space, lockType);
		}

		@Override
		public int hashCode() {
			return Objects.hash(this.lockType, this.space);
		}

		@Override
		public boolean equals(Object obj) {
			if (Validators.baseEquals(this, obj)) {
				LockPageLoader other = (LockPageLoader) obj;
				return Objects.equals(this.lockType, other.lockType) && Objects.equals(this.space, other.space);
			}
			return false;
		}
	}

	private final class UserPageLoader implements PageLoader {

		private final String alias;
		private final Space space;

		public UserPageLoader(Space space, String alias) {
			this.alias = alias;
			this.space = space;
		}

		@Override
		public Page loadFromDb() throws LogicException {
			UserPage db = userPageDao.selectBySpaceAndAlias(space, alias);
			if (db == null || !Objects.equals(SpaceContext.get(), db.getSpace())) {
				throw new LogicException(USER_PAGE_NOT_EXISTS);
			}
			return db;
		}

		@Override
		public int hashCode() {
			return Objects.hash(this.alias, this.space);
		}

		@Override
		public boolean equals(Object obj) {
			if (Validators.baseEquals(this, obj)) {
				UserPageLoader other = (UserPageLoader) obj;
				return Objects.equals(this.alias, other.alias) && Objects.equals(this.space, other.space);
			}
			return false;
		}
	}

	private final class ExpandedPageLoader implements PageLoader {

		private final Integer id;

		public ExpandedPageLoader(Integer id) {
			super();
			this.id = id;
		}

		@Override
		public Page loadFromDb() throws LogicException {
			return queryExpandedPage(id);
		}

		@Override
		public int hashCode() {
			return Objects.hash(this.id);
		}

		@Override
		public boolean equals(Object obj) {
			if (Validators.baseEquals(this, obj)) {
				ExpandedPageLoader other = (ExpandedPageLoader) obj;
				return Objects.equals(this.id, other.id);
			}
			return false;
		}
	}

	private final class FragmentQueryImpl implements FragmentQuery {

		private Space space;

		@Override
		public Fragment query(String name) {
			// 首先查找用户自定义fragment
			UserFragment userFragment = userFragmentDao.selectBySpaceAndName(space, name);
			if (userFragment == null) {
				// 查找全局
				userFragment = userFragmentDao.selectGlobalByName(name);
			}
			if (userFragment != null) {
				return userFragment;
			}
			for (Fragment fragment : fragments) {
				if (fragment.getName().equals(name)) {
					return fragment;
				}
			}
			return null;
		}

		public FragmentQueryImpl(Space space) {
			this.space = space;
		}

	}

	public void setProcessors(List<DataTagProcessor<?>> processors) {
		this.processors = processors;
	}

	public void setFragments(List<Fragment> fragments) {
		this.fragments = fragments;
	}

}
