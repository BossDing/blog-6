package me.qyh.blog.service.impl;

import java.io.InputStream;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import me.qyh.blog.config.Constants;
import me.qyh.blog.dao.ErrorPageDao;
import me.qyh.blog.dao.ExpandedPageDao;
import me.qyh.blog.dao.SpaceDao;
import me.qyh.blog.dao.SysPageDao;
import me.qyh.blog.dao.UserPageDao;
import me.qyh.blog.dao.UserWidgetDao;
import me.qyh.blog.dao.WidgetTplDao;
import me.qyh.blog.entity.Space;
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.exception.SystemException;
import me.qyh.blog.pageparam.PageResult;
import me.qyh.blog.pageparam.UserPageQueryParam;
import me.qyh.blog.pageparam.UserWidgetQueryParam;
import me.qyh.blog.service.UIService;
import me.qyh.blog.ui.Params;
import me.qyh.blog.ui.ParseResult;
import me.qyh.blog.ui.TemplateParser;
import me.qyh.blog.ui.TemplateParser.WidgetQuery;
import me.qyh.blog.ui.page.ErrorPage;
import me.qyh.blog.ui.page.ErrorPage.ErrorCode;
import me.qyh.blog.ui.page.ExpandedPage;
import me.qyh.blog.ui.page.ExpandedPageHandler;
import me.qyh.blog.ui.page.ExpandedPageServer;
import me.qyh.blog.ui.page.Page;
import me.qyh.blog.ui.page.SysPage;
import me.qyh.blog.ui.page.SysPage.PageTarget;
import me.qyh.blog.ui.page.UserPage;
import me.qyh.blog.ui.widget.SysWidgetHandler;
import me.qyh.blog.ui.widget.SysWidgetServer;
import me.qyh.blog.ui.widget.UserWidget;
import me.qyh.blog.ui.widget.Widget;
import me.qyh.blog.ui.widget.WidgetTag;
import me.qyh.blog.ui.widget.WidgetTpl;
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
	private UserWidgetDao userWidgetDao;
	@Autowired
	private WidgetTplDao widgetTplDao;
	@Autowired
	private SpaceDao spaceDao;
	@Autowired
	private ExpandedPageDao expandedPageDao;

	@Autowired
	private SysWidgetServer systemWidgetServer;
	@Autowired
	private TemplateParser templateParser;
	@Autowired
	private ExpandedPageServer expandedPageServer;

	private UICacheRender uiCacheRender;

	private Map<PageTarget, Resource> sysPageDefaultTpls = new HashMap<PageTarget, Resource>();
	private Map<PageTarget, String> _sysPageDefaultTpls = new HashMap<PageTarget, String>();

	private Map<ErrorCode, Resource> errorPageDefaultTpls = new HashMap<ErrorCode, Resource>();
	private Map<ErrorCode, String> _errorPageDefaultTpls = new HashMap<ErrorCode, String>();

	@Override
	public void insertUserWidget(UserWidget userWidget) throws LogicException {
		UserWidget db = userWidgetDao.selectByName(userWidget.getName());
		boolean nameExists = db != null;
		if (!nameExists) {
			// 检查是否和系统挂件名称重复
			nameExists = systemWidgetServer.getHandler(userWidget.getName()) != null;
		}
		if (nameExists) {
			throw new LogicException("widget.user.nameExists", "挂件名:" + userWidget.getName() + "已经存在",
					userWidget.getName());
		}
		userWidget.setCreateDate(Timestamp.valueOf(LocalDateTime.now()));
		userWidgetDao.insert(userWidget);

		uiCacheRender.evit(userWidget);
	}

	@Override
	public void deleteUserWidget(Integer id) throws LogicException {
		UserWidget userWidget = userWidgetDao.selectById(id);
		if (userWidget == null) {
			throw new LogicException("widget.user.notExists", "挂件不存在");
		}
		List<WidgetTpl> tpls = widgetTplDao.selectByWidget(userWidget);
		deleteWidgetTpls(tpls);
		userWidgetDao.deleteById(id);

		uiCacheRender.evit(userWidget);
	}

	@Override
	public void updateUserWidget(UserWidget userWidget) throws LogicException {
		UserWidget old = userWidgetDao.selectById(userWidget.getId());
		if (old == null) {
			throw new LogicException("widget.user.notExists", "挂件不存在");
		}
		UserWidget db = userWidgetDao.selectByName(userWidget.getName());
		boolean nameExists = db != null && !db.equals(userWidget);
		if (!nameExists) {
			nameExists = systemWidgetServer.getHandler(userWidget.getName()) != null;
		}
		if (nameExists) {
			throw new LogicException("widget.user.nameExists", "挂件名:" + userWidget.getName() + "已经存在",
					userWidget.getName());
		}
		userWidgetDao.update(userWidget);

		uiCacheRender.evit(old, userWidget);
	}

	@Override
	@Transactional(readOnly = true)
	public PageResult<UserWidget> queryUserWidget(UserWidgetQueryParam param) {
		int count = userWidgetDao.selectCount(param);
		List<UserWidget> datas = userWidgetDao.selectPage(param);
		return new PageResult<UserWidget>(param, count, datas);
	}

	@Override
	@Transactional(readOnly = true)
	public UserWidget queryUserWidget(Integer id) {
		return userWidgetDao.selectById(id);
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
		deletePageWidgetTpl(db);
		userPageDao.deleteById(id);
		uiCacheRender.evit(db);
	}

	@Override
	@Transactional(readOnly = true)
	public List<WidgetTpl> parseWidget(SysPage page) throws LogicException {
		SysPage db = sysPageDao.selectBySpaceAndPageTarget(page.getSpace(), page.getTarget());
		ParseResult result = templateParser.parse(page.getTpl(), new WidgetQueryImpl(db == null ? page : db, false));
		return result.getTpls();
	}

	@Override
	@Transactional(readOnly = true)
	public List<WidgetTpl> parseWidget(UserPage page) throws LogicException {
		UserPage db = userPageDao.selectById(page.getId());
		ParseResult result = templateParser.parse(page.getTpl(), new WidgetQueryImpl(db == null ? page : db, false));
		return result.getTpls();
	}

	@Override
	@Transactional(readOnly = true)
	public SysPage querySysPage(Space space, PageTarget target) {
		SysPage sysPage = sysPageDao.selectBySpaceAndPageTarget(space, target);
		if (sysPage == null) {
			sysPage = new SysPage(space, target);
			sysPage.setTpl(_sysPageDefaultTpls.get(target));
			sysPage.setTarget(target);
		}
		sysPage.setSpace(space);
		return sysPage;
	}

	@Override
	@Transactional(readOnly = true)
	public void renderPreviewPage(SysPage page) throws LogicException {
		checkSpace(page.getSpace());
		SysPage db = sysPageDao.selectBySpaceAndPageTarget(page.getSpace(), page.getTarget());
		_renderPreviewPage(page, db == null ? page : db);
	}

	@Override
	@Transactional(readOnly = true)
	public SysPage renderPreviewPage(Space space, PageTarget target) throws LogicException {
		checkSpace(space);
		SysPage page = querySysPage(space, target);
		return uiCacheRender.renderPreview(page);
	}

	@Override
	@Transactional(readOnly = true)
	public void renderPreviewPage(UserPage page) throws LogicException {
		checkSpace(page.getSpace());
		UserPage db = userPageDao.selectById(page.getId());
		_renderPreviewPage(page, db == null ? page : db);
	}

	private void _renderPreviewPage(Page preview, Page db) throws LogicException {
		ParseResult result = templateParser.parse(preview.getTpl(), new WidgetQueryImpl(db, true));
		preview.setTpl(result.getPageTpl());
		List<WidgetTpl> widgetTpls = result.getTpls();
		List<WidgetTpl> _widgetTpls = preview.getTpls();
		if (!CollectionUtils.isEmpty(widgetTpls) && !CollectionUtils.isEmpty(_widgetTpls)) {
			for (WidgetTpl widgetTpl : widgetTpls) {
				for (WidgetTpl _widgetTpl : _widgetTpls) {
					if (widgetTpl.getWidget().equals(_widgetTpl.getWidget())) {
						widgetTpl.setTpl(_widgetTpl.getTpl());
					}
				}
			}
		}
		preview.setTpls(widgetTpls);
	}

	private void deleteWidgetTpls(List<WidgetTpl> tpls) {
		if (!tpls.isEmpty()) {
			for (WidgetTpl tpl : tpls) {
				if (tpl.hasId()) {
					widgetTplDao.deleteById(tpl.getId());
				}
			}
		}
	}

	@Override
	@Transactional(readOnly = true)
	public SysPage renderSysPage(Space space, PageTarget pageTarget, Params params) throws LogicException {
		SysPage db = querySysPage(space, pageTarget);
		return uiCacheRender.render(db, params);
	}

	@Override
	public UserPage renderUserPage(String idOrAlias) throws LogicException {
		UserPage db;
		try {
			Integer id = Integer.parseInt(idOrAlias);
			db = userPageDao.selectById(id);
		} catch (NumberFormatException e) {
			db = userPageDao.selectByAlias(idOrAlias);
		}
		if (db == null) {
			throw new LogicException("page.user.notExists", "自定义页面不存在");
		}
		Space space = SpaceContext.get();
		if ((space == null && db.getSpace() != null) || (space != null && !space.equals(db.getSpace()))) {
			throw new LogicException("page.user.notExists", "自定义页面不存在");
		}
		return uiCacheRender.render(db, new Params());
	}

	@Override
	public void buildTpl(SysPage sysPage) throws LogicException {
		checkSpace(sysPage.getSpace());
		SysPage db = sysPageDao.selectBySpaceAndPageTarget(sysPage.getSpace(), sysPage.getTarget());
		boolean update = db != null;
		if (update) {
			sysPage.setId(db.getId());
			sysPageDao.update(sysPage);
		} else {
			sysPageDao.insert(sysPage);
		}
		updateWidget(sysPage);
		uiCacheRender.evit(sysPage);
	}

	@Override
	public void buildTpl(UserPage userPage) throws LogicException {
		checkSpace(userPage.getSpace());
		String alias = userPage.getAlias();
		if (alias != null) {
			UserPage aliasPage = userPageDao.selectByAlias(alias);
			if (aliasPage != null && !aliasPage.equals(userPage)) {
				throw new LogicException("page.user.aliasExists", "别名" + alias + "已经存在", alias);
			}
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
		} else {
			userPageDao.insert(userPage);
		}
		updateWidget(userPage);
		uiCacheRender.evit(userPage);
	}

	@Override
	public void deleteWidgetTpl(Page page, Widget widget) {
		WidgetTpl widgetTpl = widgetTplDao.selectByPageAndWidget(page, widget);
		if (widgetTpl != null) {
			deleteWidgetTpls(Arrays.asList(widgetTpl));
			uiCacheRender.evit(page);
		}
	}

	@Override
	public void deleteSysPage(Space space, PageTarget target) throws LogicException {
		SysPage page = sysPageDao.selectBySpaceAndPageTarget(space, target);
		if (page != null) {
			deletePageWidgetTpl(page);
			sysPageDao.deleteById(page.getId());
			uiCacheRender.evit(page);
		}
	}

	@Override
	@Transactional(readOnly = true)
	public ExpandedPage renderExpandedPage(Integer id, Params params) throws LogicException {
		ExpandedPage expandedPage = queryExpandedPage(id);
		return uiCacheRender.render(expandedPage, params);
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
			deletePageWidgetTpl(page);
			expandedPageDao.deleteById(id);
			uiCacheRender.evit(page);
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
		updateWidget(page);
		uiCacheRender.evit(page);
	}

	@Override
	@Transactional(readOnly = true)
	public void renderPreviewPage(ExpandedPage expandedPage) throws LogicException {
		ExpandedPage db = expandedPageDao.selectById(expandedPage.getId());
		_renderPreviewPage(expandedPage, db == null ? expandedPage : db);
	}

	@Override
	@Transactional(readOnly = true)
	public List<WidgetTpl> parseWidget(ExpandedPage page) throws LogicException {
		ExpandedPage db = expandedPageDao.selectById(page.getId());
		ParseResult result = templateParser.parse(page.getTpl(), new WidgetQueryImpl(db == null ? page : db, false));
		return result.getTpls();
	}

	@Override
	public void buildTpl(ErrorPage errorPage) throws LogicException {
		checkSpace(errorPage.getSpace());
		ErrorPage db = errorPageDao.selectBySpaceAndErrorCode(errorPage.getSpace(), errorPage.getErrorCode());
		boolean update = db != null;
		if (update) {
			errorPage.setId(db.getId());
			errorPageDao.update(errorPage);
		} else {
			errorPageDao.insert(errorPage);
		}
		updateWidget(errorPage);
		uiCacheRender.evit(errorPage);
	}

	@Override
	public void deleteErrorPage(Space space, ErrorCode errorCode) throws LogicException {
		ErrorPage page = errorPageDao.selectBySpaceAndErrorCode(space, errorCode);
		if (page != null) {
			deletePageWidgetTpl(page);
			errorPageDao.deleteById(page.getId());
			uiCacheRender.evit(page);
		}
	}

	@Override
	@Transactional(readOnly = true)
	public void renderPreviewPage(ErrorPage errorPage) throws LogicException {
		ErrorPage db = errorPageDao.selectBySpaceAndErrorCode(errorPage.getSpace(), errorPage.getErrorCode());
		_renderPreviewPage(errorPage, db == null ? errorPage : db);
	}

	@Override
	@Transactional(readOnly = true)
	public List<WidgetTpl> parseWidget(ErrorPage page) throws LogicException {
		ErrorPage db = errorPageDao.selectBySpaceAndErrorCode(page.getSpace(), page.getErrorCode());
		ParseResult result = templateParser.parse(page.getTpl(), new WidgetQueryImpl(db == null ? page : db, false));
		return result.getTpls();
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
	public ErrorPage renderErrorPage(Space space, ErrorCode code) throws LogicException {
		ErrorPage db = queryErrorPage(space, code);
		return uiCacheRender.render(db, new Params());
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (CollectionUtils.isEmpty(sysPageDefaultTpls)) {
			throw new SystemException("系统页面默认模板不能为空");
		}
		for (PageTarget target : PageTarget.values()) {
			if (!sysPageDefaultTpls.containsKey(target)) {
				throw new SystemException("系统页面：" + target + "没有设置默认的模板");
			}
			Resource resource = sysPageDefaultTpls.get(target);
			InputStream is = null;
			try {
				is = resource.getInputStream();
				String tpl = IOUtils.toString(is, Constants.CHARSET);
				if (tpl.length() > PageValidator.PAGE_TPL_MAX_LENGTH) {
					throw new SystemException("系统页面：" + target + "模板不能超过" + PageValidator.PAGE_TPL_MAX_LENGTH + "个字符");
				}
				if (Validators.isEmptyOrNull(tpl, true)) {
					throw new SystemException("系统页面：" + target + "模板不能为空");
				}
				_sysPageDefaultTpls.put(target, tpl);
			} catch (Exception e) {
				throw new SystemException(e.getMessage(), e);
			} finally {
				IOUtils.closeQuietly(is);
			}
		}
		for (ErrorCode code : ErrorCode.values()) {
			if (!errorPageDefaultTpls.containsKey(code)) {
				throw new SystemException("错误页面状态：" + code + "没有设置默认的模板");
			}
			Resource resource = errorPageDefaultTpls.get(code);
			InputStream is = null;
			try {
				is = resource.getInputStream();
				String tpl = IOUtils.toString(is, Constants.CHARSET);
				if (tpl.length() > PageValidator.PAGE_TPL_MAX_LENGTH) {
					throw new SystemException("错误页面：" + code + "模板不能超过" + PageValidator.PAGE_TPL_MAX_LENGTH + "个字符");
				}
				if (Validators.isEmptyOrNull(tpl, true)) {
					throw new SystemException("错误页面：" + code + "模板不能为空");
				}
				_errorPageDefaultTpls.put(code, tpl);
			} catch (Exception e) {
				throw new SystemException(e.getMessage(), e);
			} finally {
				IOUtils.closeQuietly(is);
			}
		}
		this.uiCacheRender = new UICacheRender();
	}

	private final class WidgetQueryImpl implements WidgetQuery {

		private Page page;
		private boolean test;

		public WidgetQueryImpl(Page page, boolean test) {
			this.page = page;
			this.test = test;
		}

		@Override
		public WidgetTpl query(WidgetTag widgetTag) throws LogicException {
			String name = widgetTag.getName();
			Widget widget = null;
			SysWidgetHandler sysWidgetHandler = systemWidgetServer.getHandler(name);
			if (sysWidgetHandler != null) {
				if (test) {
					widget = sysWidgetHandler.getTestWidget();
				} else {
					widget = sysWidgetHandler.getWidget();
				}
			} else {
				widget = userWidgetDao.selectByName(name);
			}
			if (widget != null) {
				WidgetTpl tpl = widgetTplDao.selectByPageAndWidget(page, widget);
				if (tpl == null) {
					tpl = new WidgetTpl();
					tpl.setTpl(widget.getDefaultTpl());
				}
				tpl.setPage(page);
				tpl.setWidget(widget);
				return tpl;
			}
			return null;
		}
	}

	private void deletePageWidgetTpl(Page page) throws LogicException {
		// 解析当前页面模板，获取当前页面包含的挂件并删除
		deleteWidgetTpls(templateParser.parse(page.getTpl(), new WidgetQueryImpl(page, false)).getTpls());
		// 删除页面历史挂件模板
		deleteWidgetTpls(widgetTplDao.selectByPage(page));
	}

	private void updateWidget(Page page) throws LogicException {
		// 解析当前页面模板
		ParseResult result = templateParser.parse(page.getTpl(), new WidgetQueryImpl(page, false));
		List<WidgetTpl> widgetTpls = result.getTpls();
		// 如果包含挂件
		if (!CollectionUtils.isEmpty(widgetTpls)) {
			List<WidgetTpl> _widgetTpls = page.getTpls();
			if (!CollectionUtils.isEmpty(_widgetTpls)) {
				for (WidgetTpl widgetTpl : widgetTpls) {
					for (WidgetTpl _widgetTpl : _widgetTpls) {
						Widget widget = widgetTpl.getWidget();
						if (widget.equals(_widgetTpl.getWidget())) {
							// 如果页面是插入，那么所有的挂件模板也将执行插入操作
							WidgetTpl tpl = widgetTplDao.selectByPageAndWidget(page, widget);
							if (tpl == null) {
								tpl = new WidgetTpl();
								tpl.setPage(page);
								tpl.setWidget(widget);
								tpl.setTpl(_widgetTpl.getTpl());
								widgetTplDao.insert(tpl);
							} else {
								tpl.setTpl(_widgetTpl.getTpl());
								widgetTplDao.update(tpl);
							}
						}
					}
				}
			}
		}
	}

	private void checkSpace(Space space) throws LogicException {
		if (space != null && spaceDao.selectById(space.getId()) == null) {
			throw new LogicException("space.notExists", "空间不存在");
		}
	}

	public void setErrorPageDefaultTpls(Map<ErrorCode, Resource> errorPageDefaultTpls) {
		this.errorPageDefaultTpls = errorPageDefaultTpls;
	}

	public void setSysPageDefaultTpls(Map<PageTarget, Resource> sysPageDefaultTpls) {
		this.sysPageDefaultTpls = sysPageDefaultTpls;
	}

	private final class UICacheRender {

		private final ConcurrentHashMap<String, ParseResult> cache;

		public UICacheRender() {
			this.cache = new ConcurrentHashMap<>();
		}

		private ParseResult get(Page page) throws LogicException {
			ParseResult cached = cache.get(page.getTemplateName());
			if (cached == null) {
				cached = templateParser.parse(page.getTpl(), new WidgetQuery() {

					@Override
					public WidgetTpl query(WidgetTag widgetTag) throws LogicException {
						SysWidgetHandler handler = systemWidgetServer.getHandler(widgetTag.getName());
						Widget widget = null;
						if (handler != null) {
							widget = handler.getWidget();
						} else {
							widget = userWidgetDao.selectByName(widgetTag.getName());
						}
						if (widget != null) {
							WidgetTpl tpl = widgetTplDao.selectByPageAndWidget(page, widget);
							if (tpl == null) {
								tpl = new WidgetTpl();
								tpl.setTpl(widget.getDefaultTpl());
							}
							tpl.setPage(page);
							tpl.setWidget(widget);
							return tpl;
						}
						return null;
					}
				});
				cache.put(page.getTemplateName(), cached);
			}
			return cached;
		}

		public <T extends Page> T renderPreview(T page) throws LogicException {
			ParseResult cached = get(page);
			List<WidgetTpl> tpls = new ArrayList<WidgetTpl>();
			for (WidgetTpl tpl : cached.getTpls()) {
				switch (tpl.getWidget().getType()) {
				case SYSTEM:
					SysWidgetHandler sysWidgetHandler = systemWidgetServer.getHandler(tpl.getWidget().getName());
					WidgetTpl _tpl = new WidgetTpl(tpl);
					_tpl.setWidget(sysWidgetHandler.getTestWidget());
					tpls.add(_tpl);
					break;
				case USER:
					tpls.add(tpl);
					break;
				}
			}
			page.setTpl(cached.getPageTpl());
			page.setTpls(tpls);
			return page;
		}

		public <T extends Page> T render(T page, Params params) throws LogicException {
			ParseResult cached = get(page);
			List<WidgetTpl> tpls = new ArrayList<WidgetTpl>();
			for (WidgetTpl tpl : cached.getTpls()) {
				switch (tpl.getWidget().getType()) {
				case SYSTEM:
					SysWidgetHandler sysWidgetHandler = systemWidgetServer.getHandler(tpl.getWidget().getName());
					Space space = page.getSpace();
					if (params != null && sysWidgetHandler.canProcess(space, params)) {
						WidgetTpl _tpl = new WidgetTpl(tpl);
						// 从系统挂件中获取数据
						_tpl.setWidget(sysWidgetHandler.getWidget(space, params));
						tpls.add(_tpl);
					}
					// 如果挂件不存在，那么交给TplResolver处理
					break;
				case USER:
					// 用户自定义挂件直接添加，它本身即是模板也是数据
					tpls.add(tpl);
					break;
				}
			}
			page.setTpl(cached.getPageTpl());
			page.setTpls(tpls);
			return page;
		}

		public void evit(Page page) {
			cache.remove(page.getTemplateName());
		}

		public void evit(Widget... widgets) {
			for (Map.Entry<String, ParseResult> it : cache.entrySet()) {
				ParseResult st = it.getValue();
				labe1: for (WidgetTpl tpl : st.getTpls()) {
					for (Widget widget : widgets) {
						if (widget.equals(tpl.getWidget())) {
							cache.remove(it.getKey());
							break labe1;
						}
					}
				}
				label2: for (String name : st.getUnkownWidgets()) {
					for (Widget widget : widgets) {
						if (name.equals(widget.getName())) {
							cache.remove(it.getKey());
							break label2;
						}
					}
				}
			}
		}
	}
}
