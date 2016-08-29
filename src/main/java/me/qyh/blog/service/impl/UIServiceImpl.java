package me.qyh.blog.service.impl;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.core.io.Resource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.engine.TemplateManager;

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
import me.qyh.blog.message.Message;
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

	@Autowired
	private TemplateEngine templateEngine;

	private static final String DEFAULT_UI_CACHE_NAME = "uiCache";
	private String uiCacheName = DEFAULT_UI_CACHE_NAME;

	@Autowired
	private CacheManager cacheManager;
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
			throw new LogicException(new Message("widget.user.nameExists", "挂件名:" + userWidget.getName() + "已经存在",
					userWidget.getName()));
		}
		userWidgetDao.insert(userWidget);
	}

	@Override
	public void deleteUserWidget(Integer id) throws LogicException {
		UserWidget userWidget = userWidgetDao.selectById(id);
		if (userWidget == null) {
			throw new LogicException(new Message("widget.user.notExists", "挂件不存在"));
		}
		List<WidgetTpl> tpls = widgetTplDao.selectByWidget(userWidget);
		deleteWidgetTpls(tpls);
		userWidgetDao.deleteById(id);
	}

	@Override
	public void updateUserWidget(UserWidget userWidget) throws LogicException {
		if (userWidgetDao.selectById(userWidget.getId()) == null) {
			throw new LogicException(new Message("widget.user.notExists", "挂件不存在"));
		}
		UserWidget db = userWidgetDao.selectByName(userWidget.getName());
		boolean nameExists = db != null && !db.equals(userWidget);
		if (!nameExists) {
			nameExists = systemWidgetServer.getHandler(userWidget.getName()) != null;
		}
		if (nameExists) {
			throw new LogicException(new Message("widget.user.nameExists", "挂件名:" + userWidget.getName() + "已经存在",
					userWidget.getName()));
		}
		userWidgetDao.update(userWidget);
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
			throw new LogicException(new Message("page.user.notExists", "自定义页面不存在"));
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
		checkSpace(page);
		SysPage db = sysPageDao.selectBySpaceAndPageTarget(page.getSpace(), page.getTarget());
		_renderPreviewPage(page, db == null ? page : db);
	}

	@Override
	@Transactional(readOnly = true)
	public void renderPreviewPage(UserPage page) throws LogicException {
		checkSpace(page);
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
			TemplateManager templateManager = templateEngine.getConfiguration().getTemplateManager();
			for (WidgetTpl tpl : tpls) {
				if (tpl.hasId()) {
					widgetTplDao.deleteById(tpl.getId());
				}
				templateManager.clearCachesFor(tpl.getTemplateName());
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
		} catch (Exception e) {
			db = userPageDao.selectByAlias(idOrAlias);
		}
		if (db == null) {
			throw new LogicException(new Message("page.user.notExists", "自定义页面不存在"));
		}
		Space space = SpaceContext.get();
		if ((space == null && db.getSpace() != null) || (space != null && !space.equals(db.getSpace()))) {
			throw new LogicException(new Message("page.user.notExists", "自定义页面不存在"));
		}
		return uiCacheRender.render(db, new Params());
	}

	public static void main(String[] args) {
		System.out.println(StringUtils.isNumeric("58.8"));
	}

	@Override
	public void buildTpl(SysPage sysPage) throws LogicException {
		checkSpace(sysPage);
		SysPage db = sysPageDao.selectBySpaceAndPageTarget(sysPage.getSpace(), sysPage.getTarget());
		boolean update = db != null;
		if (update) {
			sysPage.setId(db.getId());
			sysPageDao.update(sysPage);
			clearPageCache(db);
		} else {
			sysPageDao.insert(sysPage);
		}
		updateWidget(sysPage);
		uiCacheRender.evit(sysPage);
	}

	@Override
	public void buildTpl(UserPage userPage) throws LogicException {
		checkSpace(userPage);
		String alias = userPage.getAlias();
		if (alias != null) {
			UserPage aliasPage = userPageDao.selectByAlias(alias);
			if (aliasPage != null && !aliasPage.equals(userPage)) {
				throw new LogicException(new Message("page.user.aliasExists", "别名" + alias + "已经存在", alias));
			}
		}
		boolean update = userPage.hasId();
		if (update) {
			UserPage db = userPageDao.selectById(userPage.getId());
			if (db == null) {
				throw new LogicException(new Message("page.user.notExists", "自定义页面不存在"));
			}
			userPage.setId(db.getId());
			userPageDao.update(userPage);
			clearPageCache(db);
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
				throw new LogicException(new Message("page.expanded.notExists", "拓展页面不存在"));
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
			throw new LogicException(new Message("page.expanded.notExists", "拓展页面不存在"));
		}
		ExpandedPage db = expandedPageDao.selectById(page.getId());
		boolean update = (db != null);
		if (update) {
			page.setId(db.getId());
			expandedPageDao.update(page);
			clearPageCache(page);
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
		checkSpace(errorPage);
		ErrorPage db = errorPageDao.selectBySpaceAndErrorCode(errorPage.getSpace(), errorPage.getErrorCode());
		boolean update = db != null;
		if (update) {
			errorPage.setId(db.getId());
			errorPageDao.update(errorPage);
			clearPageCache(db);
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

	public void setSysPageDefaultTpls(Map<PageTarget, Resource> sysPageDefaultTpls) {
		this.sysPageDefaultTpls = sysPageDefaultTpls;
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
				_errorPageDefaultTpls.put(code, tpl);
			} catch (Exception e) {
				throw new SystemException(e.getMessage(), e);
			} finally {
				IOUtils.closeQuietly(is);
			}
		}
		if (uiCacheName == null) {
			uiCacheName = DEFAULT_UI_CACHE_NAME;
		}
		Cache cache = cacheManager.getCache(uiCacheName);
		if (uiCacheName == null) {
			throw new SystemException("无法找到名为" + uiCacheName + "的UI缓存");
		}
		this.uiCacheRender = new UICacheRender(cache);
	}

	private final class WidgetQueryImpl implements WidgetQuery {

		private Page page;
		private Params params;
		private boolean test;

		public WidgetQueryImpl(Page page, boolean test) {
			this.page = page;
			this.test = test;
		}

		public WidgetQueryImpl(Page page, Params params) {
			this.page = page;
			this.params = params;
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
					if (params == null) {
						widget = sysWidgetHandler.getWidget();
					}
					Space space = page.getSpace();
					if (params != null && sysWidgetHandler.canProcess(space, params)) {
						widget = sysWidgetHandler.getWidget(space, params);
					}
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
		// 清除页面模板缓存
		templateEngine.getConfiguration().getTemplateManager().clearCachesFor(page.getTemplateName());
	}

	private void clearPageCache(Page page) throws LogicException {
		// 清除旧页面模板缓存
		List<WidgetTpl> tpls = templateParser.parse(page.getTpl(), new WidgetQueryImpl(page, false)).getTpls();
		TemplateManager templateManager = templateEngine.getConfiguration().getTemplateManager();
		if (!CollectionUtils.isEmpty(tpls)) {
			for (WidgetTpl widgetTpl : tpls) {
				templateManager.clearCachesFor(widgetTpl.getTemplateName());
			}
		}
		templateManager.clearCachesFor(page.getTemplateName());
	}

	private void updateWidget(Page page) throws LogicException {
		// 解析当前页面模板
		ParseResult result = templateParser.parse(page.getTpl(), new WidgetQueryImpl(page, false));
		List<WidgetTpl> widgetTpls = result.getTpls();
		// 如果包含挂件
		if (!CollectionUtils.isEmpty(widgetTpls)) {
			List<WidgetTpl> _widgetTpls = page.getTpls();
			if (!CollectionUtils.isEmpty(_widgetTpls)) {
				TemplateManager templateManager = templateEngine.getConfiguration().getTemplateManager();
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
								templateManager.clearCachesFor(tpl.getTemplateName());
							}
						}
					}
				}
			}
		}
	}

	private void checkSpace(Page page) throws LogicException {
		Space space = page.getSpace();
		if (space != null && spaceDao.selectById(space.getId()) == null) {
			throw new LogicException(new Message("space.notExists", "空间不存在"));
		}
	}

	public void setErrorPageDefaultTpls(Map<ErrorCode, Resource> errorPageDefaultTpls) {
		this.errorPageDefaultTpls = errorPageDefaultTpls;
	}

	public void setUiCacheName(String uiCacheName) {
		this.uiCacheName = uiCacheName;
	}

	private final class UICacheRender {

		private Cache cache;

		public UICacheRender(Cache cache) {
			this.cache = cache;
		}

		public <T extends Page> T render(T page, Params params) throws LogicException {
			SimpleTemplate cached = cache.get(page.getTemplateName(), SimpleTemplate.class);
			if (cached == null) {
				ParseResult result = templateParser.parse(page.getTpl(), new WidgetQueryImpl(page, params));
				SimpleTemplate st = new SimpleTemplate();
				st.tpl = result.getPageTpl();
				if (!CollectionUtils.isEmpty(result.getTpls())) {
					List<SimpleWidgetTemplate> swts = new ArrayList<>();
					for (WidgetTpl tpl : result.getTpls()) {
						swts.add(new SimpleWidgetTemplate(tpl));
					}
					st.widgetTpls = swts;
				}
				cache.put(page.getTemplateName(), st);
				page.setTpl(result.getPageTpl());
				page.setTpls(result.getTpls());
			} else {
				List<WidgetTpl> tpls = new ArrayList<WidgetTpl>();
				if (!CollectionUtils.isEmpty(cached.widgetTpls)) {
					for (SimpleWidgetTemplate tpl : cached.widgetTpls) {
						Widget widget = null;
						SysWidgetHandler sysWidgetHandler = systemWidgetServer.getHandler(tpl.name);
						if (sysWidgetHandler != null) {
							Space space = page.getSpace();
							widget = sysWidgetHandler.getWidget(space, params);
						} else {
							widget = userWidgetDao.selectByName(tpl.name);
						}
						if (widget == null) {
							// 通常是因为用户删除了挂件
							// 由于挂件所在页面的不确定性，当挂件发生变更后需要清空所有ui的缓存
							// 为了不这么做，这里只有在缓存解析过程中发生变更后才清空这个页面的缓存,虽然这可能会导致最多查询两次
							cache.evict(page.getTemplateName());
							return render(page, params);
						}
						WidgetTpl widgetTpl = new WidgetTpl();
						widgetTpl.setId(tpl.id);
						// 判断是否覆盖了挂件模板，如果没有覆盖，那么需要从原始模板中覆盖，因为期间原始模板(用户挂件模板)可能会发生变动
						if (!widgetTpl.hasId()) {
							widgetTpl.setTpl(widget.getDefaultTpl());
						} else {
							widgetTpl.setTpl(tpl.tpl);
						}
						widgetTpl.setWidget(widget);
						widgetTpl.setPage(page);
						tpls.add(widgetTpl);
					}
				}
				page.setTpl(cached.tpl);
				page.setTpls(tpls);
			}
			return page;
		}

		private class SimpleTemplate {
			private String tpl;
			private List<SimpleWidgetTemplate> widgetTpls = new ArrayList<SimpleWidgetTemplate>();
		}

		private class SimpleWidgetTemplate {
			private Integer id;
			private String name;
			private String tpl;

			public SimpleWidgetTemplate(WidgetTpl tpl) {
				this.id = tpl.getId();
				this.name = tpl.getWidget().getName();
				this.tpl = tpl.getTpl();
			}
		}

		public void evit(Page page) {
			cache.evict(page.getTemplateName());
		}
	}

}
