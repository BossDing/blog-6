package me.qyh.blog.service.impl;

import java.io.InputStream;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

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
import me.qyh.blog.dao.SpaceDao;
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
import me.qyh.blog.web.controller.form.PageValidator;
import me.qyh.blog.web.interceptor.SpaceContext;
import me.qyh.util.Validators;

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
	private SpaceDao spaceDao;
	@Autowired
	private ExpandedPageDao expandedPageDao;
	@Autowired
	private ExpandedPageServer expandedPageServer;
	@Autowired
	private LockManager lockManager;

	private UICacheRender uiCacheRender;

	private Map<PageTarget, Resource> sysPageDefaultTpls = new HashMap<PageTarget, Resource>();
	private Map<PageTarget, String> _sysPageDefaultTpls = new HashMap<PageTarget, String>();

	private Map<ErrorCode, Resource> errorPageDefaultTpls = new HashMap<ErrorCode, Resource>();
	private Map<ErrorCode, String> _errorPageDefaultTpls = new HashMap<ErrorCode, String>();

	private Map<String, String> lockPageDefaultTpls = new HashMap<String, String>();

	private final TemplateParser templateParser = new TemplateParser();

	private List<DataTagProcessor<?>> processors = new ArrayList<DataTagProcessor<?>>();

	private final DataQuery previewDataQuery = new DataQuery() {

		@Override
		public DataBind<?> query(DataTag dataTag) throws LogicException {
			DataTagProcessor<?> processor = geTagProcessor(dataTag.getName());
			if (processor != null)
				return processor.previewData(dataTag.getAttrs());
			return null;
		}
	};

	/**
	 * 系统默认片段
	 */
	private List<Fragment> fragments = new ArrayList<Fragment>();

	@Override
	public void insertUserFragment(UserFragment userFragment) throws LogicException {
		Space space = userFragment.getSpace();
		if (space != null && spaceDao.selectById(space.getId()) == null) {
			throw new LogicException("space.notExists", "空间不存在");
		}
		UserFragment db = null;
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
		if (space != null && spaceDao.selectById(space.getId()) == null) {
			throw new LogicException("space.notExists", "空间不存在");
		}
		UserFragment old = userFragmentDao.selectById(userFragment.getId());
		if (old == null) {
			throw new LogicException("fragment.user.notExists", "挂件不存在");
		}
		UserFragment db = null;
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
		return userPageDao.selectByAlias(alias);
	}

	@Override
	@Transactional(readOnly = true)
	public PageResult<UserPage> queryUserPage(UserPageQueryParam param) {
		int count = userPageDao.selectCount(param);
		List<UserPage> datas = userPageDao.selectPage(param);
		return new PageResult<UserPage>(param, count, datas);
	}

	@Override
	public void deleteUserPage(Integer id) throws LogicException {
		UserPage db = userPageDao.selectById(id);
		if (db == null) {
			throw new LogicException("page.user.notExists", "自定义页面不存在");
		}
		userPageDao.deleteById(id);
		uiCacheRender.evit(db.getTemplateName());
	}

	@Override
	@Transactional(readOnly = true)
	public SysPage querySysPage(Space space, PageTarget target) {
		SysPage sysPage = sysPageDao.selectBySpaceAndPageTarget(space, target);
		if (sysPage == null) {
			sysPage = new SysPage(space, target);
			sysPage.setTpl(_sysPageDefaultTpls.get(target));
		}
		sysPage.setSpace(space);
		return sysPage;
	}

	@Override
	public List<String> queryDataTags() {
		List<String> dataTags = new ArrayList<>(processors.size());
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
		Space db = spaceDao.selectById(space.getId());
		if (db == null) {
			throw new LogicException("space.notExists", "空间不存在");
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
	public RenderedPage renderUserPage(String alias) throws LogicException {
		return uiCacheRender.render(new UserPageLoader(alias), new Params());
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
		uiCacheRender.evit(sysPage.getTemplateName());
	}

	@Override
	public void buildTpl(UserPage userPage) throws LogicException {
		checkSpace(userPage);
		String alias = userPage.getAlias();
		UserPage aliasPage = userPageDao.selectByAlias(alias);
		if (aliasPage != null && !aliasPage.equals(userPage)) {
			throw new LogicException("page.user.aliasExists", "别名" + alias + "已经存在", alias);
		}
		userPage.setCreateDate(Timestamp.valueOf(LocalDateTime.now()));
		boolean update = userPage.hasId();
		if (update) {
			UserPage db = userPageDao.selectById(userPage.getId());
			if (db == null) {
				throw new LogicException("page.user.notExists", "自定义页面不存在");
			}
			userPage.setId(db.getId());
			userPageDao.update(userPage);
			uiCacheRender.evit(db.getTemplateName());
		} else {
			userPageDao.insert(userPage);
		}
	}

	@Override
	public void deleteSysPage(Space space, PageTarget target) throws LogicException {
		SysPage page = sysPageDao.selectBySpaceAndPageTarget(space, target);
		if (page != null) {
			sysPageDao.deleteById(page.getId());
			uiCacheRender.evit(page.getTemplateName());
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
		List<ExpandedPage> pages = new ArrayList<ExpandedPage>();
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
			uiCacheRender.evit(page.getTemplateName());
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
		boolean update = (db != null);
		if (update) {
			page.setId(db.getId());
			expandedPageDao.update(page);
		} else {
			expandedPageDao.insert(page);
		}
		uiCacheRender.evit(page.getTemplateName());
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
		uiCacheRender.evit(lockPage.getTemplateName());
	}

	@Override
	public void deleteLockPage(Space space, String lockType) throws LogicException {
		LockPage page = lockPageDao.selectBySpaceAndLockType(space, lockType);
		if (page != null) {
			lockPageDao.deleteById(page.getId());
			uiCacheRender.evit(page.getTemplateName());
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
		return uiCacheRender.render(new LockPageRender(lockType, space), new Params());
	}

	private void checkLockType(String lockType) throws LogicException {
		if (!lockManager.checkLockTypeExists(lockType))
			throw new LogicException("page.lock.locktype.notexists", "锁类型：" + lockType + "不存在", lockType);
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
		uiCacheRender.evit(errorPage.getTemplateName());
	}

	@Override
	public void deleteErrorPage(Space space, ErrorCode errorCode) throws LogicException {
		ErrorPage page = errorPageDao.selectBySpaceAndErrorCode(space, errorCode);
		if (page != null) {
			errorPageDao.deleteById(page.getId());
			uiCacheRender.evit(page.getTemplateName());
		}
	}

	@Override
	@Transactional(readOnly = true)
	public ErrorPage queryErrorPage(Space space, ErrorCode code) {
		ErrorPage db = errorPageDao.selectBySpaceAndErrorCode(space, code);
		if (db == null) {
			db = new ErrorPage(space, code);
			db.setTpl(_errorPageDefaultTpls.get(code));
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
		final Space sp = space == null ? null : spaceDao.selectById(space.getId());
		if (space != null && sp == null)
			throw new LogicException("space.notExists", "空间不存在");
		List<ExportPage> pages = new ArrayList<ExportPage>();
		// 系统页面
		for (PageTarget target : PageTarget.values()) {
			RenderedPage page = uiCacheRender.renderPreview(new SysPageLoader(target, sp));
			ExportPage ep = new ExportPage();
			ep.setPage(new SysPage(null, target));
			ep.getPage().setTpl(page.getPage().getTpl());
			ep.setFragments(new ArrayList<>(page.getFragmentMap().values()));
			pages.add(ep);
		}
		// 错误页面
		for (ErrorCode errorCode : ErrorCode.values()) {
			RenderedPage page = uiCacheRender.renderPreview(new ErrorPageLoader(errorCode, sp));
			ExportPage ep = new ExportPage();
			ep.setPage(new ErrorPage(null, errorCode));
			ep.getPage().setTpl(page.getPage().getTpl());
			ep.setFragments(new ArrayList<>(page.getFragmentMap().values()));
			pages.add(ep);
		}
		// 个人页面
		for (UserPage up : userPageDao.selectBySpace(sp)) {
			RenderedPage page = uiCacheRender.renderPreview(new PageLoader() {

				@Override
				public String pageKey() {
					return up.getTemplateName();
				}

				@Override
				public Page loadFromDb() throws LogicException {
					return up;
				}
			});
			ExportPage ep = new ExportPage();
			ep.setPage(new UserPage(up.getId()));
			ep.getPage().setTpl(page.getPage().getTpl());
			ep.setFragments(new ArrayList<>(page.getFragmentMap().values()));
			pages.add(ep);
		}
		// 解锁
		for (String lockType : lockManager.allTypes()) {
			RenderedPage page = uiCacheRender.renderPreview(new LockPageRender(lockType, sp));
			ExportPage ep = new ExportPage();
			ep.setPage(new LockPage(null, lockType));
			ep.getPage().setTpl(page.getPage().getTpl());
			ep.setFragments(new ArrayList<>(page.getFragmentMap().values()));
			pages.add(ep);
		}
		if (req.isExportExpandedPage()) {
			for (ExpandedPage ep : expandedPageDao.selectAll()) {
				RenderedPage page = uiCacheRender.renderPreview(new ExpandedPageLoader(ep.getId()));
				ExportPage ep2 = new ExportPage();
				ep2.setPage(new ExpandedPage(ep.getId()));
				ep2.getPage().setTpl(page.getPage().getTpl());
				ep2.setFragments(new ArrayList<>(page.getFragmentMap().values()));
				pages.add(ep2);
			}
		}
		return pages;
	}

	@Override
	public ImportResult importTemplate(List<ImportPageWrapper> wrappers, ImportReq req) throws LogicException {
		Space space = req.getSpace();
		if (space != null) {
			space = spaceDao.selectById(space.getId());
			if (space == null)
				throw new LogicException("space.notExists", "空间不存在");
		}
		ImportResult result = new ImportResult();
		for (ImportPageWrapper ipw : wrappers) {
			ExportPage ep = ipw.getPage();
			Page page = ep.getPage();

			Page db = null;
			switch (ep.getPage().getType()) {
			case USER:
				db = userPageDao.selectById(page.getId());
				if (db != null) {
					// 检查空间是否一致
					if (!Objects.equals(space, db.getSpace())) {
						String spaceName = space == null ? "" : space.getName();
						result.addError(new ImportError(ipw.getIndex(),
								new Message("tpl.import.pageNotInSpace",
										"页面[" + page.getType() + "," + page.getId() + "]不在空间" + spaceName + "中",
										page.getType(), page.getId(), spaceName)));
						continue;
					}
				}
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
			final Page loaderPage = db;
			// 渲染以前的页面模板用于保存
			RenderedPage old = uiCacheRender.renderPreview(new PageLoader() {

				@Override
				public String pageKey() {
					return loaderPage.getTemplateName();
				}

				@Override
				public Page loadFromDb() throws LogicException {
					return loaderPage;
				}
			});
			result.addOldPage(new ExportPage(old.getPage(), new ArrayList<>(old.getFragmentMap().values())));
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
		// 清空页面缓存
		for (ExportPage oldPage : result.getOldPages()) {
			uiCacheRender.evit(oldPage.getPage().getTemplateName());
		}
		return result;
	}

	@Override
	@Transactional(readOnly = true)
	public DataBind<?> queryData(DataTag dataTag) throws LogicException {
		DataTagProcessor<?> processor = geTagProcessor(dataTag.getName());
		if (processor != null)
			return processor.getData(SpaceContext.get(), new Params(), dataTag.getAttrs());
		return null;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		for (PageTarget target : PageTarget.values()) {
			Resource resource = sysPageDefaultTpls.get(target);
			if (resource == null)
				resource = new ClassPathResource("me/qyh/blog/ui/page/PAGE_" + target.name() + ".html");
			String tpl = fromResource(resource);
			if (tpl.length() > PageValidator.PAGE_TPL_MAX_LENGTH) {
				throw new SystemException("系统页面：" + target + "模板不能超过" + PageValidator.PAGE_TPL_MAX_LENGTH + "个字符");
			}
			if (Validators.isEmptyOrNull(tpl, true)) {
				throw new SystemException("系统页面：" + target + "模板不能为空");
			}
			_sysPageDefaultTpls.put(target, tpl);
		}
		for (ErrorCode code : ErrorCode.values()) {
			Resource resource = errorPageDefaultTpls.get(code);
			if (resource == null)
				resource = new ClassPathResource("me/qyh/blog/ui/page/" + code.name() + ".html");
			String tpl = fromResource(resource);
			if (tpl.length() > PageValidator.PAGE_TPL_MAX_LENGTH) {
				throw new SystemException("错误页面：" + code + "模板不能超过" + PageValidator.PAGE_TPL_MAX_LENGTH + "个字符");
			}
			if (Validators.isEmptyOrNull(tpl, true)) {
				throw new SystemException("错误页面：" + code + "模板不能为空");
			}
			_errorPageDefaultTpls.put(code, tpl);
		}
		for (String lockType : lockManager.allTypes()) {
			Resource resource = lockManager.getDefaultTemplateResource(lockType);
			if (resource == null)
				throw new SystemException("没有指定LockType:" + lockType + "的默认模板");
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
		InputStream is = null;
		try {
			is = resource.getInputStream();
			return IOUtils.toString(is, Constants.CHARSET);
		} catch (Exception e) {
			throw new SystemException(e.getMessage(), e);
		} finally {
			IOUtils.closeQuietly(is);
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
			space = spaceDao.selectById(space.getId());
			if (space == null)
				throw new LogicException("space.notExists", "空间不存在");
			page.setSpace(space);
		}
	}

	private final class UICacheRender {

		private final ConcurrentHashMap<String, ParseResultWrapper> cache;

		public UICacheRender() {
			this.cache = new ConcurrentHashMap<>();
		}

		private ParseResultWrapper get(PageLoader loader) throws LogicException {
			String key = loader.pageKey();
			ParseResultWrapper cached = cache.get(key);
			if (cached == null) {
				final Page db = loader.loadFromDb();
				ParseResult parseResult = templateParser.parse(db.getTpl(), noDataQuery,
						new FragmentQueryImpl(db.getSpace()));
				cached = new ParseResultWrapper(parseResult, db);
				cache.put(key, cached);
			}
			return cached;
		}

		public RenderedPage renderPreview(PageLoader loader) throws LogicException {
			ParseResultWrapper cached = get(loader);
			ParseResult result = cached.parseResult;
			List<DataBind<?>> binds = new ArrayList<>();
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
			List<DataBind<?>> binds = new ArrayList<>();
			for (DataTag unkownData : result.getUnkownDatas()) {
				//
				DataTagProcessor<?> processor = geTagProcessor(unkownData.getName());
				if (processor != null) {
					binds.add(processor.getData(cached.page.getSpace(), params, unkownData.getAttrs()));
				}
			}
			return new RenderedPage((Page) cached.page.clone(), binds, result.getFragments());
		}

		public void evit(String key) {
			cache.remove(key);
		}

		public void evit(Fragment... fragments) {
			for (Map.Entry<String, ParseResultWrapper> it : cache.entrySet()) {
				ParseResultWrapper st = it.getValue();
				Map<String, Fragment> currentFMap = st.parseResult.getFragments();
				labe1: for (Fragment currentF : currentFMap.values()) {
					for (Fragment fragment : fragments) {
						if (fragment.getName().equals(currentF.getName())) {
							cache.remove(it.getKey());
							break labe1;
						}
					}
				}
				label2: for (String name : st.parseResult.getUnkownFragments()) {
					for (Fragment fragment : fragments) {
						if (name.equals(fragment.getName())) {
							cache.remove(it.getKey());
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
		String pageKey();

		Page loadFromDb() throws LogicException;
	}

	private final class SysPageLoader implements PageLoader {
		private PageTarget target;
		private Space space;

		@Override
		public String pageKey() {
			return new SysPage(space, target).getTemplateName();
		}

		@Override
		public Page loadFromDb() throws LogicException {
			return querySysPage(space, target);
		}

		public SysPageLoader(PageTarget target, Space space) {
			super();
			this.target = target;
			this.space = space;
		}

	}

	private final class ErrorPageLoader implements PageLoader {
		private ErrorCode errorCode;
		private Space space;

		@Override
		public String pageKey() {
			return new ErrorPage(space, errorCode).getTemplateName();
		}

		@Override
		public Page loadFromDb() throws LogicException {
			return queryErrorPage(space, errorCode);
		}

		public ErrorPageLoader(ErrorCode errorCode, Space space) {
			this.errorCode = errorCode;
			this.space = space;
		}

	}

	private final class LockPageRender implements PageLoader {
		private String lockType;
		private Space space;

		@Override
		public String pageKey() {
			return new LockPage(space, lockType).getTemplateName();
		}

		@Override
		public Page loadFromDb() throws LogicException {
			return queryLockPage(space, lockType);
		}

		public LockPageRender(String lockType, Space space) {
			this.lockType = lockType;
			this.space = space;
		}

	}

	private final class UserPageLoader implements PageLoader {

		private String alias;

		@Override
		public String pageKey() {
			return new UserPage(alias).getTemplateName();
		}

		@Override
		public Page loadFromDb() throws LogicException {
			UserPage db = userPageDao.selectByAlias(alias);
			if (db == null) {
				throw new LogicException("page.user.notExists", "自定义页面不存在");
			}
			Space space = SpaceContext.get();
			if ((space == null && db.getSpace() != null) || (space != null && !space.equals(db.getSpace()))) {
				throw new LogicException("page.user.notExists", "自定义页面不存在");
			}
			return db;
		}

		public UserPageLoader(String alias) {
			this.alias = alias;
		}
	}

	private final class ExpandedPageLoader implements PageLoader {

		private Integer id;

		@Override
		public String pageKey() {
			return new ExpandedPage(id).getTemplateName();
		}

		@Override
		public Page loadFromDb() throws LogicException {
			return queryExpandedPage(id);
		}

		public ExpandedPageLoader(Integer id) {
			super();
			this.id = id;
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
			if (userFragment != null)
				return userFragment;
			for (Fragment fragment : fragments) {
				if (fragment.getName().equals(name))
					return fragment;
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

	@Override
	@Transactional(readOnly = true)
	public Fragment queryFragment(String name) {
		return new FragmentQueryImpl(SpaceContext.get()).query(name);
	}

}
