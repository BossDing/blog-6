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
package me.qyh.blog.core.service.impl;

import java.io.IOException;
import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.util.CollectionUtils;

import me.qyh.blog.core.bean.ExportPage;
import me.qyh.blog.core.bean.ImportOption;
import me.qyh.blog.core.bean.ImportRecord;
import me.qyh.blog.core.config.Constants;
import me.qyh.blog.core.dao.LockPageDao;
import me.qyh.blog.core.dao.SysPageDao;
import me.qyh.blog.core.dao.UserFragmentDao;
import me.qyh.blog.core.dao.UserPageDao;
import me.qyh.blog.core.entity.Space;
import me.qyh.blog.core.evt.EventType;
import me.qyh.blog.core.evt.UserPageEvent;
import me.qyh.blog.core.exception.LogicException;
import me.qyh.blog.core.exception.RuntimeLogicException;
import me.qyh.blog.core.exception.SystemException;
import me.qyh.blog.core.lock.LockManager;
import me.qyh.blog.core.message.Message;
import me.qyh.blog.core.pageparam.PageResult;
import me.qyh.blog.core.pageparam.UserFragmentQueryParam;
import me.qyh.blog.core.pageparam.UserPageQueryParam;
import me.qyh.blog.core.service.ConfigService;
import me.qyh.blog.core.service.UIService;
import me.qyh.blog.core.ui.DataTag;
import me.qyh.blog.core.ui.Template;
import me.qyh.blog.core.ui.TemplateEvitEvent;
import me.qyh.blog.core.ui.TemplateUtils;
import me.qyh.blog.core.ui.data.DataBind;
import me.qyh.blog.core.ui.data.DataTagProcessor;
import me.qyh.blog.core.ui.fragment.Fragment;
import me.qyh.blog.core.ui.fragment.UserFragment;
import me.qyh.blog.core.ui.page.LockPage;
import me.qyh.blog.core.ui.page.Page;
import me.qyh.blog.core.ui.page.SysPage;
import me.qyh.blog.core.ui.page.SysPage.PageTarget;
import me.qyh.blog.core.ui.page.UserPage;
import me.qyh.blog.util.Resources;
import me.qyh.blog.util.Times;
import me.qyh.blog.util.Validators;
import me.qyh.blog.web.GetRequestMappingRegisterEvent;
import me.qyh.blog.web.GetRequestMappingUnRegisterEvent;
import me.qyh.blog.web.controller.UserPageController;
import me.qyh.blog.web.controller.form.PageValidator;

public class UIServiceImpl implements UIService, InitializingBean, ApplicationEventPublisherAware {

	@Autowired
	private SysPageDao sysPageDao;
	@Autowired
	private UserPageDao userPageDao;
	@Autowired
	private LockPageDao lockPageDao;
	@Autowired
	private UserFragmentDao userFragmentDao;
	@Autowired
	private SpaceCache spaceCache;
	@Autowired
	private LockManager lockManager;
	@Autowired
	private ConfigService configService;

	@Autowired
	private PlatformTransactionManager platformTransactionManager;
	private ApplicationEventPublisher applicationEventPublisher;

	private Map<PageTarget, Resource> sysPageDefaultTpls = new EnumMap<>(PageTarget.class);
	private Map<PageTarget, String> sysPageDefaultParsedTpls = new EnumMap<>(PageTarget.class);
	private Map<String, String> lockPageDefaultTpls = new HashMap<>();
	private List<DataTagProcessor<?>> processors = new ArrayList<>();

	private static final Message USER_PAGE_NOT_EXISTS = new Message("page.user.notExists", "自定义页面不存在");

	private static final Logger LOGGER = LoggerFactory.getLogger(UIServiceImpl.class);

	/**
	 * 系统默认片段
	 */
	private List<Fragment> fragments = new ArrayList<>();

	@Override
	@Sync
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	public void insertUserFragment(UserFragment userFragment) throws LogicException {
		checkSpace(userFragment);
		UserFragment db;
		if (userFragment.isGlobal()) {
			db = userFragmentDao.selectGlobalByName(userFragment.getName());
		} else {
			db = userFragmentDao.selectBySpaceAndName(userFragment.getSpace(), userFragment.getName());
		}
		boolean nameExists = db != null;
		if (nameExists) {
			throw new LogicException("fragment.user.nameExists", "挂件名:" + userFragment.getName() + "已经存在",
					userFragment.getName());
		}

		userFragment.setCreateDate(Timestamp.valueOf(LocalDateTime.now()));
		userFragmentDao.insert(userFragment);
		evitFragmentCache(userFragment.getName());
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	public void deleteUserFragment(Integer id) throws LogicException {
		UserFragment userFragment = userFragmentDao.selectById(id);
		if (userFragment == null) {
			throw new LogicException("fragment.user.notExists", "挂件不存在");
		}
		userFragmentDao.deleteById(id);

		evitFragmentCache(userFragment.getName());
	}

	@Override
	@Sync
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	public void updateUserFragment(UserFragment userFragment) throws LogicException {
		checkSpace(userFragment);
		UserFragment old = userFragmentDao.selectById(userFragment.getId());
		if (old == null) {
			throw new LogicException("fragment.user.notExists", "挂件不存在");
		}
		UserFragment db;
		// 查找当前数据库是否存在同名
		if (userFragment.isGlobal()) {
			db = userFragmentDao.selectGlobalByName(userFragment.getName());
		} else {
			db = userFragmentDao.selectBySpaceAndName(userFragment.getSpace(), userFragment.getName());
		}
		boolean nameExists = db != null && !db.getId().equals(userFragment.getId());
		if (nameExists) {
			throw new LogicException("fragment.user.nameExists", "挂件名:" + userFragment.getName() + "已经存在",
					userFragment.getName());
		}
		userFragmentDao.update(userFragment);
		if (old.getName().endsWith(userFragment.getName())) {
			evitFragmentCache(old.getName());
		} else {
			evitFragmentCache(old.getName(), userFragment.getName());
		}
	}

	@Override
	@Transactional(readOnly = true)
	public PageResult<UserFragment> queryUserFragment(UserFragmentQueryParam param) {
		param.setPageSize(configService.getGlobalConfig().getUserFragmentPageSize());
		int count = userFragmentDao.selectCount(param);
		List<UserFragment> datas = userFragmentDao.selectPage(param);
		return new PageResult<>(param, count, datas);
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<UserFragment> queryUserFragment(Integer id) {
		return Optional.ofNullable(userFragmentDao.selectById(id));
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<UserPage> queryUserPage(Integer id) {
		return Optional.ofNullable(userPageDao.selectById(id));
	}

	@Override
	@Transactional(readOnly = true)
	public PageResult<UserPage> queryUserPage(UserPageQueryParam param) {
		param.setPageSize(configService.getGlobalConfig().getUserPagePageSize());
		int count = userPageDao.selectCount(param);
		List<UserPage> datas = userPageDao.selectPage(param);
		return new PageResult<>(param, count, datas);
	}

	@Override
	@Sync
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	public void deleteUserPage(Integer id) throws LogicException {
		final UserPageRequestMappingRegisterHelper helper = new UserPageRequestMappingRegisterHelper();
		UserPage db = userPageDao.selectById(id);
		if (db == null) {
			throw new LogicException(USER_PAGE_NOT_EXISTS);
		}
		userPageDao.deleteById(id);
		String templateName = db.getTemplateName();
		evitTemplateCache(templateName);
		this.applicationEventPublisher.publishEvent(new UserPageEvent(this, EventType.DELETE, db));
		helper.unregisterUserPage(db);
	}

	@Override
	public List<String> queryDataTags() {
		return processors.stream().map(processor -> processor.getName()).collect(Collectors.toList());
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	public void buildTpl(SysPage sysPage) throws LogicException {
		checkPageTarget(sysPage);
		checkSpace(sysPage);
		SysPage db = sysPageDao.selectBySpaceAndPageTarget(sysPage.getSpace(), sysPage.getTarget());
		boolean update = db != null;
		if (update) {
			sysPage.setId(db.getId());
			sysPageDao.update(sysPage);
		} else {
			sysPageDao.insert(sysPage);
		}
		evitTemplateCache(sysPage);
	}

	@Override
	@Sync
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	public void buildTpl(UserPage userPage) throws LogicException {
		final UserPageRequestMappingRegisterHelper helper = new UserPageRequestMappingRegisterHelper();
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
			UserPage aliasPage = userPageDao.selectBySpaceAndAlias(userPage.getSpace(), alias);
			if (aliasPage != null && !aliasPage.getId().equals(userPage.getId())) {
				throw new LogicException("page.user.aliasExists", "别名" + alias + "已经存在", alias);
			}
			userPageDao.update(userPage);

			evitTemplateCache(db);

			// 解除以前的mapping
			helper.unregisterUserPage(db);
		} else {
			// 检查
			UserPage aliasPage = userPageDao.selectBySpaceAndAlias(userPage.getSpace(), alias);
			if (aliasPage != null) {
				throw new LogicException("page.user.aliasExists", "别名" + alias + "已经存在", alias);
			}
			userPageDao.insert(userPage);
		}

		// 注册现在的页面
		helper.registerUserPage(userPage);

		EventType type = update ? EventType.UPDATE : EventType.INSERT;
		this.applicationEventPublisher.publishEvent(new UserPageEvent(this, type, userPage));
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	public void deleteSysPage(Integer spaceId, PageTarget target) throws LogicException {
		Space space = spaceCache.checkSpace(spaceId);
		SysPage page = sysPageDao.selectBySpaceAndPageTarget(space, target);
		if (page != null) {
			sysPageDao.deleteById(page.getId());

			evitTemplateCache(page);
		}
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	public void buildTpl(LockPage lockPage) throws LogicException {
		checkPageTarget(lockPage);
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
		evitTemplateCache(lockPage);
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	public void deleteLockPage(Integer spaceId, String lockType) throws LogicException {
		Space space = spaceCache.checkSpace(spaceId);
		LockPage page = lockPageDao.selectBySpaceAndLockType(space, lockType);
		if (page != null) {
			lockPageDao.deleteById(page.getId());
			evitTemplateCache(page);
		}
	}

	private void checkLockType(String lockType) throws LogicException {
		if (!lockManager.checkLockTypeExists(lockType)) {
			throw new LogicException("page.lock.locktype.notexists", "锁类型：" + lockType + "不存在", lockType);
		}
	}

	@Override
	public Optional<DataBind<?>> queryData(DataTag dataTag, boolean onlyCallable) throws LogicException {
		Optional<DataTagProcessor<?>> processor = getTagProcessor(dataTag.getName());
		if (onlyCallable) {
			processor = processor.filter(DataTagProcessor::isCallable);
		}
		if (processor.isPresent()) {
			return Optional.of(processor.get().getData(dataTag.getAttrs()));
		}
		return Optional.empty();
	}

	@Override
	public Optional<DataBind<?>> queryPreviewData(DataTag dataTag) {
		return getTagProcessor(dataTag.getName()).map(processor -> processor.previewData(dataTag.getAttrs()));
	}

	@Override
	@Transactional(readOnly = true)
	public Template queryTemplate(String templateName) {
		if (TemplateUtils.isPageTemplate(templateName)) {
			return queryPageWithTemplateName(templateName).orElse(null);
		} else {
			return queryFragmentWithTemplateName(templateName).orElse(null);
		}
	}

	private ExportPage exportPage(String templateName) {
		Page page = queryPageWithTemplateName(templateName)
				.orElseThrow(() -> new SystemException("页面" + templateName + "不粗真乃"));
		return export(page);
	}

	@Override
	@Transactional(readOnly = true)
	public List<ExportPage> exportPage(Integer spaceId) throws LogicException {
		Space space = spaceCache.checkSpace(spaceId);
		List<ExportPage> exportPages = new ArrayList<>();
		// sys
		for (PageTarget pageTarget : PageTarget.values()) {
			exportPages.add(exportPage(new SysPage(space, pageTarget).getTemplateName()));
		}
		// lock
		for (String lockType : lockManager.allTypes()) {
			exportPages.add(exportPage(new LockPage(space, lockType).getTemplateName()));
		}
		// User
		for (UserPage page : userPageDao.selectBySpace(space)) {
			exportPages.add(export(page));
		}
		return exportPages;
	}

	@Override
	@Transactional(readOnly = true)
	public List<UserPage> selectAllUserPages() {
		return userPageDao.selectAll();
	}

	@Override
	@Sync
	public List<ImportRecord> importPage(Integer spaceId, List<ExportPage> exportPages, ImportOption importOption) {
		if (CollectionUtils.isEmpty(exportPages)) {
			return new ArrayList<>();
		}
		DefaultTransactionDefinition td = new DefaultTransactionDefinition();
		td.setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);
		td.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		TransactionStatus ts = platformTransactionManager.getTransaction(td);
		List<ImportRecord> records = new ArrayList<>();
		UserPageRequestMappingRegisterHelper helper = new UserPageRequestMappingRegisterHelper();
		try {
			Space space = null;
			try {
				space = spaceCache.checkSpace(spaceId);
			} catch (LogicException e) {
				ts.setRollbackOnly();
				return Arrays.asList(new ImportRecord(false, e.getLogicMessage()));
			}
			if (importOption == null) {
				importOption = new ImportOption();
			}
			Set<String> pageEvitKeySet = new HashSet<>();
			Set<String> fragmentEvitKeySet = new HashSet<>();
			for (ExportPage exportPage : exportPages) {
				Page page = exportPage.getPage();
				page.setSpace(space);
				String templateName = page.getTemplateName();
				Optional<Page> optional = queryPageWithTemplateName(templateName);
				if (!optional.isPresent()) {
					if (page instanceof UserPage) {
						// 如果插入用户页面
						if (importOption.isCreateUserPageIfNotExists()) {
							UserPage userPage = (UserPage) page;
							userPage.setCreateDate(Timestamp.valueOf(LocalDateTime.now()));
							userPage.setDescription("");
							userPage.setName(userPage.getAlias());
							userPage.setSpace(space);
							userPage.setAllowComment(false);

							userPageDao.insert(userPage);

							try {
								helper.registerUserPage(userPage);
							} catch (LogicException ex) {
								records.add(new ImportRecord(true, ((LogicException) ex).getLogicMessage()));
								ts.setRollbackOnly();
								return records;
							}

							records.add(new ImportRecord(true, new Message("import.insert.tpl.success",
									"插入模板" + templateName + "成功", templateName)));
							continue;
						}
						records.add(new ImportRecord(false, USER_PAGE_NOT_EXISTS));
						ts.setRollbackOnly();
						return records;
					}
					throw new SystemException("模板" + templateName + "对应的页面不存在");
				}
				Page current = optional.get();
				// 如果页面内容发生了改变
				if (!checkChangeWhenImport(current, page)) {
					current.setTpl(page.getTpl());
					// 是否是更新操作
					boolean update = false;
					switch (page.getType()) {
					case SYSTEM:
						SysPage sysPage = (SysPage) current;
						if (sysPage.hasId()) {
							sysPageDao.update(sysPage);
							update = true;
						} else {
							sysPageDao.insert(sysPage);
						}
						break;
					case LOCK:
						LockPage lockPage = (LockPage) current;
						if (current.hasId()) {
							lockPageDao.update(lockPage);
							update = true;
						} else {
							lockPageDao.insert(lockPage);
						}
						break;
					case USER:
						UserPage userPage = (UserPage) current;
						userPageDao.update(userPage);
						helper.unregisterUserPage(userPage);
						try {
							helper.registerUserPage(userPage);
						} catch (LogicException ex) {
							records.add(new ImportRecord(true, ((LogicException) ex).getLogicMessage()));
							ts.setRollbackOnly();
							return records;
						}
						update = true;
						break;
					default:
						break;
					}

					if (update) {
						records.add(new ImportRecord(true,
								new Message("import.update.tpl.success", "模板" + templateName + "更新成功", templateName)));
					} else {
						records.add(new ImportRecord(true,
								new Message("import.insert.tpl.success", "模板" + templateName + "插入成功", templateName)));
					}

					pageEvitKeySet.add(templateName);
				} else {
					records.add(new ImportRecord(true,
							new Message("import.tpl.nochange", "模板" + templateName + "内容没有发生变化，无需更新", templateName)));
				}

				for (Fragment fragment : exportPage.getFragments()) {
					String fragmentName = fragment.getName();
					if (fragmentEvitKeySet.contains(fragmentName)) {
						continue;
					}
					fragment.setSpace(space);
					// 查询当前的fragment
					Optional<UserFragment> optionalFragment = queryFragmentWithTemplateName(fragment.getTemplateName());
					if (!optionalFragment.isPresent()) {
						// 插入fragment
						insertUserFragmentWhenImport(fragment, records);
					} else {
						UserFragment currentFragment = optionalFragment.get();
						if (currentFragment.getTpl().equals(fragment.getTpl())) {
							records.add(new ImportRecord(true, new Message("import.tpl.nochange",
									"模板" + fragmentName + "内容没有发生变化，无需更新", fragmentName)));
							continue;
						}
						// 如果用户存在同名的fragment，但是是global的，则插入space级别的
						if (currentFragment.isGlobal()) {
							insertUserFragmentWhenImport(fragment, records);
						} else {
							currentFragment.setTpl(fragment.getTpl());
							userFragmentDao.update(currentFragment);
							records.add(new ImportRecord(true, new Message("import.update.tpl.success",
									"模板" + fragmentName + "更新成功", fragmentName)));
						}
					}
					fragmentEvitKeySet.add(fragment.getName());
				}
			}
			evitTemplateCache(pageEvitKeySet.stream().toArray(i -> new String[i]));
			evitFragmentCache(fragmentEvitKeySet.stream().toArray(i -> new String[i]));
			return records;
		} catch (RuntimeException | Error e) {
			LOGGER.error(e.getMessage(), e);
			ts.setRollbackOnly();
			records.add(new ImportRecord(true, Constants.SYSTEM_ERROR));
			return records;
		} finally {
			platformTransactionManager.commit(ts);
		}
	}

	private boolean checkChangeWhenImport(Page old, Page current) {
		if (current.getTpl().equals(old.getTpl())) {
			return true;
		}
		return false;
	}

	@EventListener
	public void handleContextRefreshEvent(ContextRefreshedEvent evt) throws IOException {
		// servlet context
		if (evt.getApplicationContext().getParent() != null) {

			// insert login page
			if (userPageDao.selectBySpaceAndAlias(null, "login") == null) {
				UserPage page = new UserPage();
				page.setAlias("login");
				page.setName("login");
				page.setCreateDate(Timestamp.valueOf(LocalDateTime.now()));
				page.setDescription("");
				page.setAllowComment(false);
				page.setTpl(Resources.readResourceToString(new ClassPathResource("resources/page/LOGIN.html")));
				userPageDao.insert(page);
			}

			List<UserPage> allPage = userPageDao.selectAll();
			for (UserPage page : allPage) {
				this.applicationEventPublisher.publishEvent(new UserPageGetRequestMappingRegisterEvent(this, page));
			}
		}
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		this.applicationEventPublisher = applicationEventPublisher;
	}

	private void insertUserFragmentWhenImport(Fragment toImport, List<ImportRecord> records) {
		UserFragment userFragment = new UserFragment();
		userFragment.setCreateDate(Timestamp.valueOf(LocalDateTime.now()));
		userFragment.setDescription("");
		userFragment.setGlobal(false);
		userFragment.setName(toImport.getName());
		userFragment.setTpl(toImport.getTpl());
		userFragment.setSpace(toImport.getSpace());
		userFragmentDao.insert(userFragment);
		records.add(new ImportRecord(true,
				new Message("import.insert.tpl.success", "模板" + toImport.getName() + "插入成功", toImport.getName())));
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		for (PageTarget target : PageTarget.values()) {
			Resource resource = sysPageDefaultTpls.get(target);
			if (resource == null) {
				resource = new ClassPathResource("resources/page/PAGE_" + target.name() + ".html");
			}
			String tpl = Resources.readResourceToString(resource);
			if (Validators.isEmptyOrNull(tpl, true)) {
				throw new SystemException("系统页面：" + target + "模板不能为空");
			}
			if (tpl.length() > PageValidator.PAGE_TPL_MAX_LENGTH) {
				throw new SystemException("系统页面：" + target + "模板不能超过" + PageValidator.PAGE_TPL_MAX_LENGTH + "个字符");
			}
			sysPageDefaultParsedTpls.put(target, tpl);
		}
		for (String lockType : lockManager.allTypes()) {
			Resource resource = lockManager.getDefaultTemplateResource(lockType);
			if (resource == null) {
				throw new SystemException("没有指定LockType:" + lockType + "的默认模板");
			}
			String tpl = Resources.readResourceToString(resource);
			if (Validators.isEmptyOrNull(tpl, true)) {
				throw new SystemException("解锁页面：" + lockType + "模板不能为空");
			}
			if (tpl.length() > PageValidator.PAGE_TPL_MAX_LENGTH) {
				throw new SystemException("解锁页面：" + lockType + "模板不能超过" + PageValidator.PAGE_TPL_MAX_LENGTH + "个字符");
			}
			lockPageDefaultTpls.put(lockType, tpl);
		}

		TransactionStatus status = platformTransactionManager.getTransaction(new DefaultTransactionDefinition());
		try {
			Timestamp now = Timestamp.valueOf(Times.now());
			for (Fragment fragment : fragments) {
				UserFragment userFragment = userFragmentDao.selectGlobalByName(fragment.getName());
				if (userFragment == null) {
					userFragment = new UserFragment();
					userFragment.setCallable(fragment.isCallable());
					userFragment.setCreateDate(now);
					userFragment.setDescription("");
					userFragment.setGlobal(true);
					userFragment.setName(fragment.getName());
					userFragment.setSpace(null);
					userFragment.setTpl(fragment.getTpl());
					userFragmentDao.insert(userFragment);
				}
			}
		} catch (RuntimeException | Error e) {
			status.setRollbackOnly();
			throw e;
		} finally {
			platformTransactionManager.commit(status);
		}
	}

	public void setSysPageDefaultTpls(Map<PageTarget, Resource> sysPageDefaultTpls) {
		this.sysPageDefaultTpls = sysPageDefaultTpls;
	}

	private Optional<DataTagProcessor<?>> getTagProcessor(String name) {
		return processors.stream().filter(processor -> processor.getName().equals(name)).findAny();
	}

	private void checkSpace(Page page) throws LogicException {
		Space space = page.getSpace();
		if (space != null) {
			page.setSpace(spaceCache.checkSpace(space.getId()));
		}
	}

	private void checkSpace(UserFragment userFragment) throws LogicException {
		Space space = userFragment.getSpace();
		if (space != null) {
			userFragment.setSpace(spaceCache.checkSpace(space.getId()));
		}
	}

	private synchronized void evitTemplateCache(String... templateNames) {
		Transactions.afterCommit(() -> {
			this.applicationEventPublisher.publishEvent(new TemplateEvitEvent(this, templateNames));
		});
	}

	private void evitTemplateCache(Page... pages) {
		evitTemplateCache(Arrays.stream(pages).map(Page::getTemplateName).toArray(i -> new String[i]));
	}

	private void checkPageTarget(Page page) throws LogicException {
		if (page.getSpace() == null) {
			switch (page.getType()) {
			case SYSTEM:
				SysPage sysPage = (SysPage) page;
				if (PageTarget.ARTICLE_DETAIL.equals(sysPage.getTarget())) {
					// 默认空间无法配置解锁页面和文章详情页
					throw new LogicException("page.noneed", "这个页面无需配置");
				}
				break;
			default:
				break;
			}
		}
	}

	private synchronized void evitFragmentCache(String... names) {
		Transactions.afterCommit(() -> {
			if (names == null || names.length == 0) {
				return;
			}
			List<Space> spaces = spaceCache.getSpaces(true);
			Set<String> templateNames = new HashSet<>();
			for (String name : names) {
				templateNames.add(new Fragment(name).getTemplateName());
				for (Space space : spaces) {
					templateNames.add(new Fragment(name, space).getTemplateName());
				}
			}
			this.applicationEventPublisher
					.publishEvent(new TemplateEvitEvent(this, templateNames.stream().toArray(i -> new String[i])));
		});
	}

	private Optional<UserFragment> queryFragmentWithTemplateName(String template) {
		Fragment fromTemplateName = TemplateUtils.toFragment(template);
		UserFragment userFragment = userFragmentDao.selectBySpaceAndName(fromTemplateName.getSpace(),
				fromTemplateName.getName());
		if (userFragment == null) { // 查找全局
			userFragment = userFragmentDao.selectGlobalByName(fromTemplateName.getName());
		}
		return Optional.ofNullable(userFragment);
	}

	private Optional<Page> queryPageWithTemplateName(String template) {
		Page converted = TemplateUtils.toPage(template);
		switch (converted.getType()) {
		case SYSTEM:
			SysPage sysPage = (SysPage) converted;
			converted = querySysPage(sysPage.getSpace(), sysPage.getTarget());
			break;
		case LOCK:
			LockPage lockPage = (LockPage) converted;
			converted = queryLockPage(lockPage.getSpace(), lockPage.getLockType());
			break;
		case USER:
			UserPage userPage = (UserPage) converted;
			converted = userPageDao.selectBySpaceAndAlias(userPage.getSpace(), userPage.getAlias());
			break;
		default:
			throw new SystemException("无法确定" + converted.getType() + "的页面类型");
		}
		return Optional.ofNullable(converted);
	}

	private SysPage querySysPage(Space space, PageTarget target) {
		SysPage sysPage = sysPageDao.selectBySpaceAndPageTarget(space, target);
		if (sysPage == null) {
			sysPage = new SysPage(space, target);
			sysPage.setTpl(sysPageDefaultParsedTpls.get(target));
		}
		sysPage.setSpace(space);
		return sysPage;
	}

	private LockPage queryLockPage(Space space, String lockType) {
		if (!lockManager.checkLockTypeExists(lockType)) {
			return null;
		}
		LockPage lockPage = lockPageDao.selectBySpaceAndLockType(space, lockType);
		if (lockPage == null) {
			lockPage = new LockPage(space, lockType);
			lockPage.setTpl(lockPageDefaultTpls.get(lockType));
		}
		lockPage.setSpace(space);
		return lockPage;
	}

	public void setProcessors(List<DataTagProcessor<?>> processors) {
		for (DataTagProcessor<?> processor : processors) {
			if (!Validators.isLetterOrNumOrChinese(processor.getName())) {
				throw new SystemException("数据名只能为中英文或者数字");
			}
			if (!Validators.isLetter(processor.getDataName())) {
				throw new SystemException("数据dataName只能为英文字母");
			}
		}
		this.processors = processors;
	}

	public void setFragments(List<Fragment> fragments) {
		this.fragments = fragments;
	}

	private ExportPage export(Page page) {
		ExportPage exportPage = new ExportPage();
		exportPage.setPage(page.toExportPage());
		Map<String, Fragment> fragmentMap = new HashMap<>();
		fillMap(fragmentMap, page.getSpace(), page.getTpl());
		for (Fragment fragment : fragmentMap.values()) {
			if (fragment != null) {
				exportPage.add(fragment.toExportFragment());
			}
		}
		fragmentMap.clear();
		return exportPage;
	}

	private void fillMap(Map<String, Fragment> fragmentMap, Space space, String tpl) {
		Map<String, Fragment> fragmentMap2 = new HashMap<>();
		Document document = Jsoup.parse(tpl);
		Elements elements = document.getElementsByTag("fragment");
		for (Element element : elements) {
			String name = element.attr("name");
			if (fragmentMap.containsKey(name)) {
				continue;
			}
			Optional<UserFragment> optional = queryFragmentWithTemplateName(
					new Fragment(name, space).getTemplateName());
			fragmentMap.put(name, optional.orElse(null));
			if (optional.isPresent()) {
				fragmentMap2.put(name, optional.get());
			}
		}
		for (Map.Entry<String, Fragment> fragmentIterator : fragmentMap2.entrySet()) {
			Fragment value = fragmentIterator.getValue();
			fillMap(fragmentMap, space, value.getTpl());
		}
		fragmentMap2.clear();
	}

	private static String getRequestMappingPath(UserPage userPage) {
		String registPath = TemplateUtils.cleanUserPageAlias(userPage.getAlias());
		Space space = userPage.getSpace();
		if (space != null) {
			Objects.requireNonNull(space.getAlias());
			registPath = "/space/" + space.getAlias() + "/" + registPath;
		} else {
			registPath = "/" + registPath;
		}
		return registPath;
	}

	private final class UserPageRequestMappingRegisterHelper {

		private List<ApplicationEvent> rollBackEvents = new ArrayList<>();

		public UserPageRequestMappingRegisterHelper() {
			super();
			Transactions.afterRollback(this::rollback);
		}

		void registerUserPage(UserPage page) throws LogicException {
			try {
				applicationEventPublisher
						.publishEvent(new UserPageGetRequestMappingRegisterEvent(UIServiceImpl.this, page, true));
			} catch (RuntimeLogicException e) {
				throw e.getLogicException();
			}
			rollBackEvents.add(new UserPageGetRequestMappingUnRegisterEvent(UIServiceImpl.this, page));
		}

		void unregisterUserPage(UserPage page) {
			applicationEventPublisher
					.publishEvent(new UserPageGetRequestMappingUnRegisterEvent(UIServiceImpl.this, page));
			rollBackEvents.add(new UserPageGetRequestMappingRegisterEvent(UIServiceImpl.this, page, true));
		}

		private void rollback() {
			if (!rollBackEvents.isEmpty()) {
				for (ApplicationEvent evt : rollBackEvents) {
					applicationEventPublisher.publishEvent(evt);
				}
			}
		}
	}

	private static final class UserPageGetRequestMappingUnRegisterEvent extends GetRequestMappingUnRegisterEvent {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public UserPageGetRequestMappingUnRegisterEvent(Object source, UserPage userPage) {
			super(source, getRequestMappingPath(userPage));
		}

	}

	private static final class UserPageGetRequestMappingRegisterEvent extends GetRequestMappingRegisterEvent {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private static final Method METHOD;

		static {
			try {
				METHOD = UserPageController.class.getMethod("handleRequest", HttpServletRequest.class);
			} catch (NoSuchMethodException | SecurityException e) {
				throw new SystemException(e.getMessage(), e);
			}
		}

		public UserPageGetRequestMappingRegisterEvent(Object source, UserPage userPage) {
			super(source, getRequestMappingPath(userPage), new UserPageController(userPage), METHOD);
		}

		public UserPageGetRequestMappingRegisterEvent(Object source, UserPage userPage, boolean force) {
			super(source, getRequestMappingPath(userPage), new UserPageController(userPage), METHOD);
			setForce(force);
		}

	}
}
