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

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.util.CollectionUtils;

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;

import me.qyh.blog.bean.ExportPage;
import me.qyh.blog.bean.ImportOption;
import me.qyh.blog.bean.ImportRecord;
import me.qyh.blog.dao.ErrorPageDao;
import me.qyh.blog.dao.LockPageDao;
import me.qyh.blog.dao.SysPageDao;
import me.qyh.blog.dao.UserFragmentDao;
import me.qyh.blog.dao.UserPageDao;
import me.qyh.blog.entity.Space;
import me.qyh.blog.evt.EventType;
import me.qyh.blog.evt.UserPageEvent;
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.exception.SystemException;
import me.qyh.blog.lock.LockManager;
import me.qyh.blog.message.Message;
import me.qyh.blog.pageparam.PageResult;
import me.qyh.blog.pageparam.UserFragmentQueryParam;
import me.qyh.blog.pageparam.UserPageQueryParam;
import me.qyh.blog.security.Environment;
import me.qyh.blog.service.ConfigService;
import me.qyh.blog.service.UIService;
import me.qyh.blog.ui.ContextVariables;
import me.qyh.blog.ui.DataTag;
import me.qyh.blog.ui.TemplateUtils;
import me.qyh.blog.ui.UICacheManager;
import me.qyh.blog.ui.data.DataBind;
import me.qyh.blog.ui.data.DataTagProcessor;
import me.qyh.blog.ui.fragment.Fragment;
import me.qyh.blog.ui.fragment.UserFragment;
import me.qyh.blog.ui.page.ErrorPage;
import me.qyh.blog.ui.page.ErrorPage.ErrorCode;
import me.qyh.blog.ui.page.LockPage;
import me.qyh.blog.ui.page.Page;
import me.qyh.blog.ui.page.SysPage;
import me.qyh.blog.ui.page.SysPage.PageTarget;
import me.qyh.blog.ui.page.UserPage;
import me.qyh.blog.util.Resources;
import me.qyh.blog.util.Validators;
import me.qyh.blog.web.controller.form.PageValidator;

public class UIServiceImpl implements UIService, InitializingBean, ApplicationEventPublisherAware {

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
	private LockManager lockManager;
	@Autowired
	private ConfigService configService;

	@Autowired
	private PlatformTransactionManager platformTransactionManager;
	private ApplicationEventPublisher applicationEventPublisher;

	private static final String DATA_TAG_PROCESSOR_NAME_PATTERN = "^[A-Za-z0-9\u4E00-\u9FA5]+$";
	private static final String DATA_TAG_PROCESSOR_DATA_NAME_PATTERN = "^[A-Za-z]+$";

	private Map<PageTarget, Resource> sysPageDefaultTpls = new EnumMap<>(PageTarget.class);
	private Map<PageTarget, String> sysPageDefaultParsedTpls = new EnumMap<>(PageTarget.class);
	private Map<ErrorCode, Resource> errorPageDefaultTpls = new EnumMap<>(ErrorCode.class);
	private Map<ErrorCode, String> errorPageDefaultParsedTpls = new EnumMap<>(ErrorCode.class);
	private Map<String, String> lockPageDefaultTpls = new HashMap<>();
	private List<DataTagProcessor<?>> processors = new ArrayList<>();

	private static final Message USER_PAGE_NOT_EXISTS = new Message("page.user.notExists", "自定义页面不存在");

	private final LoadingCache<FragmentKey, Fragment> fragmentCache = Caffeine.newBuilder()
			.build(new CacheLoader<FragmentKey, Fragment>() {

				@Override
				public Fragment load(FragmentKey key) throws Exception {
					DefaultTransactionDefinition definition = new DefaultTransactionDefinition();
					definition.setReadOnly(true);
					TransactionStatus status = platformTransactionManager.getTransaction(definition);
					try {
						UserFragment userFragment = userFragmentDao.selectBySpaceAndName(key.space, key.name);
						if (userFragment == null) { // 查找全局
							userFragment = userFragmentDao.selectGlobalByName(key.name);
						}
						if (userFragment != null) {
							return userFragment;
						}
						for (Fragment fragment : fragments) {
							if (fragment.getName().equals(key.name)) {
								return fragment;
							}
						}
						throw new LogicException(new Message("fragment.not.exists", "模板片段不存在"));
					} catch (RuntimeException | Error e) {
						status.setRollbackOnly();
						throw e;
					} finally {
						platformTransactionManager.commit(status);
					}
				}
			});

	private final LoadingCache<String, Page> pageCache = Caffeine.newBuilder().build(new CacheLoader<String, Page>() {

		@Override
		public Page load(String templateName) throws Exception {
			DefaultTransactionDefinition definition = new DefaultTransactionDefinition();
			definition.setReadOnly(true);
			TransactionStatus status = platformTransactionManager.getTransaction(definition);
			try {
				Page converted = TemplateUtils.convert(templateName);
				Space space = converted.getSpace();
				if (space != null) {
					space = spaceCache.checkSpace(space.getId());
				}
				switch (converted.getType()) {
				case SYSTEM:
					SysPage sysPage = (SysPage) converted;
					return querySysPage(sysPage.getSpace(), sysPage.getTarget());
				case ERROR:
					ErrorPage errorPage = (ErrorPage) converted;
					return queryErrorPage(errorPage.getSpace(), errorPage.getErrorCode());
				case LOCK:
					LockPage lockPage = (LockPage) converted;
					return queryLockPage(lockPage.getSpace(), lockPage.getLockType());
				case USER:
					UserPage userPage = (UserPage) converted;
					UserPage db = userPageDao.selectBySpaceAndAlias(userPage.getSpace(), userPage.getAlias());
					if (db == null) {
						throw new LogicException(USER_PAGE_NOT_EXISTS);
					}
					return db;
				default:
					throw new SystemException("无法确定" + converted.getType() + "的页面类型");
				}
			} catch (RuntimeException | Error e) {
				status.setRollbackOnly();
				throw e;
			} finally {
				platformTransactionManager.commit(status);
			}
		}
	});

	/**
	 * 系统默认片段
	 */
	private List<Fragment> fragments = new ArrayList<>();

	@Override
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
		evitFragmentCache(userFragment);
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	public void deleteUserFragment(Integer id) throws LogicException {
		UserFragment userFragment = userFragmentDao.selectById(id);
		if (userFragment == null) {
			throw new LogicException("fragment.user.notExists", "挂件不存在");
		}
		userFragmentDao.deleteById(id);

		evitFragmentCache(userFragment);
	}

	@Override
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

		evitFragmentCache(old, userFragment);
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
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	public void deleteUserPage(Integer id) throws LogicException {
		UserPage db = userPageDao.selectById(id);
		if (db == null) {
			throw new LogicException(USER_PAGE_NOT_EXISTS);
		}
		userPageDao.deleteById(id);
		clearPageCache(db);
		this.applicationEventPublisher.publishEvent(new UserPageEvent(this, EventType.DELETE, db));
	}

	@Override
	public List<String> queryDataTags() {
		return processors.stream().map(processor -> processor.getName()).collect(Collectors.toList());
	}

	@Override
	public List<Fragment> querySysFragments() {
		return Collections.unmodifiableList(fragments);
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
		clearPageCache(sysPage);
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
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
			UserPage aliasPage = userPageDao.selectBySpaceAndAlias(userPage.getSpace(), alias);
			if (aliasPage != null && !aliasPage.getId().equals(userPage.getId())) {
				throw new LogicException("page.user.aliasExists", "别名" + alias + "已经存在", alias);
			}
			userPage.setId(db.getId());
			userPageDao.update(userPage);

			clearPageCache(db);

			this.applicationEventPublisher.publishEvent(new UserPageEvent(this, EventType.UPDATE,
					(UserPage) queryPage(TemplateUtils.getTemplateName(userPage))));
		} else {
			// 检查
			UserPage aliasPage = userPageDao.selectBySpaceAndAlias(userPage.getSpace(), alias);
			if (aliasPage != null) {
				throw new LogicException("page.user.aliasExists", "别名" + alias + "已经存在", alias);
			}
			userPageDao.insert(userPage);

			this.applicationEventPublisher.publishEvent(new UserPageEvent(this, EventType.INSERT,
					(UserPage) queryPage(TemplateUtils.getTemplateName(userPage))));
		}
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	public void deleteSysPage(Integer spaceId, PageTarget target) throws LogicException {
		Space space = spaceCache.checkSpace(spaceId);
		SysPage page = sysPageDao.selectBySpaceAndPageTarget(space, target);
		if (page != null) {
			sysPageDao.deleteById(page.getId());

			clearPageCache(page);
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
		clearPageCache(lockPage);
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	public void deleteLockPage(Integer spaceId, String lockType) throws LogicException {
		Space space = spaceCache.checkSpace(spaceId);
		LockPage page = lockPageDao.selectBySpaceAndLockType(space, lockType);
		if (page != null) {
			lockPageDao.deleteById(page.getId());
			clearPageCache(page);
		}
	}

	private void checkLockType(String lockType) throws LogicException {
		if (!lockManager.checkLockTypeExists(lockType)) {
			throw new LogicException("page.lock.locktype.notexists", "锁类型：" + lockType + "不存在", lockType);
		}
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
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

		clearPageCache(errorPage);
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	public void deleteErrorPage(Integer spaceId, ErrorCode errorCode) throws LogicException {
		Space space = spaceCache.checkSpace(spaceId);
		ErrorPage page = errorPageDao.selectBySpaceAndErrorCode(space, errorCode);
		if (page != null) {
			errorPageDao.deleteById(page.getId());
			clearPageCache(page);
		}
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<DataBind<?>> queryData(DataTag dataTag, ContextVariables variables) throws LogicException {
		return doQuery(dataTag, variables, false);
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<DataBind<?>> queryCallableData(DataTag dataTag, ContextVariables variables) throws LogicException {
		return doQuery(dataTag, variables, true);
	}

	@Override
	public Optional<DataBind<?>> queryPreviewData(DataTag dataTag) {
		return getTagProcessor(dataTag.getName()).map(processor -> processor.previewData(dataTag.getAttrs()));
	}

	@Override
	public Optional<Fragment> queryFragment(String name) {
		return queryFragment(Environment.getSpace().orElse(null), name);
	}

	@Override
	public Optional<Fragment> queryCallableFragment(String name) {
		return queryFragment(name).filter(Fragment::isCallable);
	}

	private Optional<Fragment> queryFragment(Space space, String name) {
		try {
			Fragment fragment = fragmentCache.get(new FragmentKey(space, name));
			return Optional.of(TemplateUtils.clone(fragment));
		} catch (CompletionException e) {
			if (e.getCause() instanceof LogicException) {
				return Optional.empty();
			}
			throw new SystemException(e.getMessage(), e);
		}
	}

	@Override
	public Page queryPage(String templateName) throws LogicException {
		try {
			return TemplateUtils.clone(pageCache.get(templateName));
		} catch (CompletionException e) {
			Throwable cause = e.getCause();
			if (cause instanceof LogicException) {
				throw (LogicException) cause;
			}
			throw new SystemException(e.getMessage(), e);
		}
	}

	@Override
	@Transactional(readOnly = true)
	public ExportPage exportPage(String templateName) throws LogicException {
		Page page = queryPage(templateName);
		return export(page);
	}

	@Override
	@Transactional(readOnly = true)
	public List<ExportPage> exportPage(Integer spaceId) throws LogicException {
		Space space = spaceCache.checkSpace(spaceId);
		List<ExportPage> exportPages = new ArrayList<>();
		// sys
		for (PageTarget pageTarget : PageTarget.values()) {
			exportPages.add(exportPage(TemplateUtils.getTemplateName(new SysPage(space, pageTarget))));
		}
		// error
		for (ErrorCode errorCode : ErrorCode.values()) {
			exportPages.add(exportPage(TemplateUtils.getTemplateName(new ErrorPage(space, errorCode))));
		}
		// lock
		for (String lockType : lockManager.allTypes()) {
			exportPages.add(exportPage(TemplateUtils.getTemplateName(new LockPage(space, lockType))));
		}
		// User
		for (UserPage page : userPageDao.selectBySpace(space)) {
			exportPages.add(export(page));
		}
		return exportPages;
	}

	@Override
	public List<ImportRecord> importPage(Integer spaceId, List<ExportPage> exportPages, ImportOption importOption) {
		TransactionStatus ts = platformTransactionManager.getTransaction(new DefaultTransactionDefinition());
		try {
			if (CollectionUtils.isEmpty(exportPages)) {
				return new ArrayList<>();
			}
			Space space = null;
			try {
				space = spaceCache.checkSpace(spaceId);
			} catch (LogicException e) {
				return Arrays.asList(new ImportRecord(false, e.getLogicMessage()));
			}
			if (importOption == null) {
				importOption = new ImportOption();
			}
			List<ImportRecord> records = new ArrayList<>();
			Set<String> pageEvitKeySet = new HashSet<>();
			Set<String> fragmentEvitKeySet = new HashSet<>();
			for (ExportPage exportPage : exportPages) {
				Page page = exportPage.getPage();
				page.setSpace(space);
				String templateName = TemplateUtils.getTemplateName(page);
				Page current;
				try {
					// 查询当前页面
					current = queryPage(templateName);
				} catch (LogicException e) {
					// 如果用户页面不存在
					if (USER_PAGE_NOT_EXISTS.getCodes()[0].equals(e.getLogicMessage().getCodes()[0])) {
						// 如果插入用户页面
						if (importOption.isCreateUserPageIfNotExists()) {
							UserPage userPage = (UserPage) page;
							userPage.setCreateDate(Timestamp.valueOf(LocalDateTime.now()));
							userPage.setDescription("");
							userPage.setName(userPage.getAlias());
							userPage.setSpace(space);
							userPageDao.insert(userPage);
							records.add(new ImportRecord(true, new Message("import.insert.tpl.success",
									"插入模板" + templateName + "成功", templateName)));
							continue;
						}
					}
					records.add(new ImportRecord(false, e.getLogicMessage()));
					// 如果忽略逻辑异常
					if (importOption.isContinueOnFailure()) {
						continue;
					}
					ts.setRollbackOnly();
					return records;
				}
				// 如果页面内容没有改变
				if (current.getTpl().equals(page.getTpl())) {
					records.add(new ImportRecord(true,
							new Message("import.tpl.nochange", "模板" + templateName + "内容没有发生变化，无需更新", templateName)));
					continue;
				}
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
				case ERROR:
					ErrorPage errorPage = (ErrorPage) current;
					if (errorPage.hasId()) {
						errorPageDao.update(errorPage);
						update = true;
					} else {
						errorPageDao.insert(errorPage);
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

				for (Fragment fragment : exportPage.getFragments()) {
					String fragmentName = fragment.getName();
					if (fragmentEvitKeySet.contains(fragmentName)) {
						continue;
					}
					// 查询当前的fragment
					Optional<Fragment> optional = queryFragment(space, fragmentName);
					if (!optional.isPresent()) {
						// 插入fragment
						insertUserFragmentWhenImport(space, fragment, records);
					} else {
						Fragment currentFragment = optional.get();
						if (currentFragment.getTpl().equals(fragment.getTpl())) {
							records.add(new ImportRecord(true, new Message("import.tpl.nochange",
									"模板" + fragmentName + "内容没有发生变化，无需更新", fragmentName)));
							continue;
						}
						if (currentFragment instanceof UserFragment) {
							UserFragment currentUserFragment = (UserFragment) currentFragment;
							// 如果用户存在同名的fragment，但是是global的，则插入space级别的
							if (currentUserFragment.isGlobal()) {
								insertUserFragmentWhenImport(space, fragment, records);
							} else {
								currentFragment.setTpl(fragment.getTpl());
								userFragmentDao.update(currentUserFragment);
								records.add(new ImportRecord(true, new Message("import.update.tpl.success",
										"模板" + fragmentName + "更新成功", fragmentName)));
							}
						} else {
							// 如果是系统fragment，则插入用户fragment
							insertUserFragmentWhenImport(space, fragment, records);
						}
					}
					fragmentEvitKeySet.add(fragment.getName());
				}
			}
			// 清空缓存
			for (String pageEvitKey : pageEvitKeySet) {
				clearPageCache(pageEvitKey);
			}
			for (String fragmentEvitKey : fragmentEvitKeySet) {

				UserFragment userFragment = new UserFragment();
				userFragment.setGlobal(false);
				userFragment.setName(fragmentEvitKey);
				userFragment.setSpace(space);

				evitFragmentCache(userFragment);
			}
			return records;
		} catch (RuntimeException | Error e) {
			ts.setRollbackOnly();
			throw e;
		} finally {
			platformTransactionManager.commit(ts);
		}
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		this.applicationEventPublisher = applicationEventPublisher;
	}

	private void insertUserFragmentWhenImport(Space space, Fragment toImport, List<ImportRecord> records) {
		UserFragment userFragment = new UserFragment();
		userFragment.setCreateDate(Timestamp.valueOf(LocalDateTime.now()));
		userFragment.setDescription("");
		userFragment.setGlobal(false);
		userFragment.setName(toImport.getName());
		userFragment.setSpace(space);
		userFragment.setTpl(toImport.getTpl());
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
			String tpl = Resources.readResourceToString(resource);
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
			String tpl = Resources.readResourceToString(resource);
			if (tpl.length() > PageValidator.PAGE_TPL_MAX_LENGTH) {
				throw new SystemException("解锁页面：" + lockType + "模板不能超过" + PageValidator.PAGE_TPL_MAX_LENGTH + "个字符");
			}
			if (Validators.isEmptyOrNull(tpl, true)) {
				throw new SystemException("解锁页面：" + lockType + "模板不能为空");
			}
			lockPageDefaultTpls.put(lockType, tpl);
		}
	}

	public void setErrorPageDefaultTpls(Map<ErrorCode, Resource> errorPageDefaultTpls) {
		this.errorPageDefaultTpls = errorPageDefaultTpls;
	}

	public void setSysPageDefaultTpls(Map<PageTarget, Resource> sysPageDefaultTpls) {
		this.sysPageDefaultTpls = sysPageDefaultTpls;
	}

	private Optional<DataTagProcessor<?>> getTagProcessor(String name) {
		return processors.stream().filter(processor -> processor.getName().equals(name)).findAny();
	}

	private Optional<DataBind<?>> doQuery(DataTag dataTag, ContextVariables variables, boolean onlyCallable)
			throws LogicException {
		Optional<DataTagProcessor<?>> processor = getTagProcessor(dataTag.getName());
		if (onlyCallable) {
			processor = processor.filter(DataTagProcessor::isCallable);
		}
		if (processor.isPresent()) {
			return Optional.of(processor.get().getData(variables, dataTag.getAttrs()));
		}
		return Optional.empty();
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

	private void clearPageCache(String templateName) {
		pageCache.invalidate(templateName);
		UICacheManager.clearCachesFor(templateName);
	}

	private void clearPageCache(Page page) {
		clearPageCache(TemplateUtils.getTemplateName(page));
	}

	private void clearFragmentCache(Fragment fragment) {
		UICacheManager.clearCachesFor(TemplateUtils.getTemplateName(fragment));
		if (fragment instanceof UserFragment) {
			UserFragment userFragment = (UserFragment) fragment;
			// 如果不是全局的，清除可能存在的全局模板缓存
			if (!userFragment.isGlobal()) {
				UserFragment uf = new UserFragment();
				uf.setGlobal(true);
				uf.setName(userFragment.getName());
				uf.setSpace(userFragment.getSpace());

				UICacheManager.clearCachesFor(TemplateUtils.getTemplateName(uf));
			}
		}
		// 清除默认模板缓存
		Fragment defaultFragment = new Fragment();
		defaultFragment.setName(fragment.getName());
		UICacheManager.clearCachesFor(TemplateUtils.getTemplateName(defaultFragment));
	}

	/*
	 * 默认空间无法配置解锁页面和文章详情页
	 */
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

	private void evitFragmentCache(Fragment... fragments) {
		if (fragments == null || fragments.length == 0) {
			return;
		}
		fragmentCache.asMap().keySet()
				.removeIf(x -> Arrays.stream(fragments).anyMatch(fragment -> x.name.equals(fragment.getName())));
		for (Fragment fragment : fragments) {
			clearFragmentCache(fragment);
		}
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

	private ErrorPage queryErrorPage(Space space, ErrorCode code) {
		ErrorPage db = errorPageDao.selectBySpaceAndErrorCode(space, code);
		if (db == null) {
			db = new ErrorPage(space, code);
			db.setTpl(errorPageDefaultParsedTpls.get(code));
		}
		return db;
	}

	private LockPage queryLockPage(Space space, String lockType) throws LogicException {
		checkLockType(lockType);
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
			if (!processor.getName().matches(DATA_TAG_PROCESSOR_NAME_PATTERN)) {
				throw new SystemException("数据名之内为中英文或者数字");
			}
			if (!processor.getDataName().matches(DATA_TAG_PROCESSOR_DATA_NAME_PATTERN)) {
				throw new SystemException("数据dataName只能为英文字母");
			}
		}
		this.processors = processors;
	}

	public void setFragments(List<Fragment> fragments) {
		this.fragments = fragments;
	}

	private final class FragmentKey {
		private final Space space;
		private final String name;

		private FragmentKey(Space space, String name) {
			super();
			this.space = space;
			this.name = name;
		}

		@Override
		public int hashCode() {
			return Objects.hash(this.space, this.name);
		}

		@Override
		public boolean equals(Object obj) {
			if (Validators.baseEquals(this, obj)) {
				FragmentKey other = (FragmentKey) obj;
				return Objects.equals(this.space, other.space) && Objects.equals(this.name, other.name);
			}
			return false;
		}
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
			Optional<Fragment> optional = queryFragment(space, name);
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
}
