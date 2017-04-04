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
package me.qyh.blog.web.thymeleaf;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.function.Predicate;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.MapBindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.condition.PatternsRequestCondition;
import org.springframework.web.servlet.mvc.condition.RequestMethodsRequestCondition;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import me.qyh.blog.core.bean.ExportPage;
import me.qyh.blog.core.bean.ImportRecord;
import me.qyh.blog.core.bean.PathTemplateLoadRecord;
import me.qyh.blog.core.config.Constants;
import me.qyh.blog.core.dao.FragmentDao;
import me.qyh.blog.core.dao.PageDao;
import me.qyh.blog.core.entity.Space;
import me.qyh.blog.core.evt.EventType;
import me.qyh.blog.core.evt.PageEvent;
import me.qyh.blog.core.exception.LogicException;
import me.qyh.blog.core.exception.SystemException;
import me.qyh.blog.core.message.Message;
import me.qyh.blog.core.pageparam.FragmentQueryParam;
import me.qyh.blog.core.pageparam.PageResult;
import me.qyh.blog.core.pageparam.TemplatePageQueryParam;
import me.qyh.blog.core.security.Environment;
import me.qyh.blog.core.service.ConfigService;
import me.qyh.blog.core.service.impl.LogicExecutor;
import me.qyh.blog.core.service.impl.SpaceCache;
import me.qyh.blog.core.service.impl.Transactions;
import me.qyh.blog.core.thymeleaf.DataTag;
import me.qyh.blog.core.thymeleaf.TemplateEvitEvent;
import me.qyh.blog.core.thymeleaf.TemplateNotFoundException;
import me.qyh.blog.core.thymeleaf.TemplateService;
import me.qyh.blog.core.thymeleaf.data.DataBind;
import me.qyh.blog.core.thymeleaf.data.DataTagProcessor;
import me.qyh.blog.core.thymeleaf.template.Fragment;
import me.qyh.blog.core.thymeleaf.template.Page;
import me.qyh.blog.core.thymeleaf.template.PathTemplate;
import me.qyh.blog.core.thymeleaf.template.Template;
import me.qyh.blog.util.FileUtils;
import me.qyh.blog.util.Resources;
import me.qyh.blog.util.Times;
import me.qyh.blog.util.UrlUtils;
import me.qyh.blog.util.Validators;
import me.qyh.blog.web.TemplateView;
import me.qyh.blog.web.controller.form.FragmentValidator;
import me.qyh.blog.web.controller.form.PageValidator;

public class TemplateServiceImpl implements TemplateService, ApplicationEventPublisherAware, InitializingBean {

	@Autowired
	private PageDao pageDao;
	@Autowired
	private FragmentDao fragmentDao;
	@Autowired
	private SpaceCache spaceCache;
	@Autowired
	private ConfigService configService;

	@Autowired
	private PlatformTransactionManager platformTransactionManager;
	private ApplicationEventPublisher applicationEventPublisher;

	private List<DataTagProcessor<?>> processors = new ArrayList<>();

	private static final Message USER_PAGE_NOT_EXISTS = new Message("page.user.notExists", "自定义页面不存在");

	private static final Logger LOGGER = LoggerFactory.getLogger(TemplateServiceImpl.class);

	/**
	 * 系统默认片段
	 */
	private List<Fragment> fragments = new ArrayList<>();

	// PathTemplate
	private Path pathTemplateRoot;
	private boolean enablePathTemplate;
	private PathTemplateService pathTemplateService;

	// TEMPLATE REGISTER
	@Autowired
	private RequestMappingHandlerMapping requestMapping;

	private TemplateMappingRegister templateMappingRegister;

	private Map<String, SystemTemplate> defaultTemplates;

	private final List<TemplateProcessor> templateProcessors = new ArrayList<>();
	private PreviewServiceImpl previewService;

	private final AtomicInteger templateIncrementId = new AtomicInteger(0);

	@Override
	public synchronized void insertFragment(Fragment fragment) throws LogicException {
		executeInTransaction(() -> {
			checkSpace(fragment);
			Fragment db;
			if (fragment.isGlobal()) {
				db = fragmentDao.selectGlobalByName(fragment.getName());
			} else {
				db = fragmentDao.selectBySpaceAndName(fragment.getSpace(), fragment.getName());
			}
			boolean nameExists = db != null;
			if (nameExists) {
				throw new LogicException("fragment.user.nameExists", "挂件名:" + fragment.getName() + "已经存在",
						fragment.getName());
			}

			fragment.setCreateDate(Timestamp.valueOf(LocalDateTime.now()));
			fragmentDao.insert(fragment);
			evitFragmentCache(fragment.getName());
		});
	}

	@Override
	public synchronized void deleteFragment(Integer id) throws LogicException {
		executeInTransaction(() -> {
			Fragment fragment = fragmentDao.selectById(id);
			if (fragment == null) {
				throw new LogicException("fragment.user.notExists", "挂件不存在");
			}
			fragmentDao.deleteById(id);

			evitFragmentCache(fragment.getName());
		});
	}

	@Override
	public synchronized void updateFragment(Fragment fragment) throws LogicException {
		executeInTransaction(() -> {
			checkSpace(fragment);
			Fragment old = fragmentDao.selectById(fragment.getId());
			if (old == null) {
				throw new LogicException("fragment.user.notExists", "挂件不存在");
			}
			Fragment db;
			// 查找当前数据库是否存在同名
			if (fragment.isGlobal()) {
				db = fragmentDao.selectGlobalByName(fragment.getName());
			} else {
				db = fragmentDao.selectBySpaceAndName(fragment.getSpace(), fragment.getName());
			}
			boolean nameExists = db != null && !db.getId().equals(fragment.getId());
			if (nameExists) {
				throw new LogicException("fragment.user.nameExists", "挂件名:" + fragment.getName() + "已经存在",
						fragment.getName());
			}
			fragmentDao.update(fragment);
			if (old.getName().endsWith(fragment.getName())) {
				evitFragmentCache(old.getName());
			} else {
				evitFragmentCache(old.getName(), fragment.getName());
			}
		});
	}

	@Override
	public PageResult<Fragment> queryFragment(FragmentQueryParam param) {
		return Transactions.executeInReadOnlyTransaction(platformTransactionManager, status -> {
			param.setPageSize(configService.getGlobalConfig().getFragmentPageSize());
			int count = fragmentDao.selectCount(param);
			List<Fragment> datas = fragmentDao.selectPage(param);
			return new PageResult<>(param, count, datas);
		});
	}

	@Override
	public Optional<Fragment> queryFragment(Integer id) {
		return Transactions.executeInReadOnlyTransaction(platformTransactionManager, status -> {
			return Optional.ofNullable(fragmentDao.selectById(id));
		});
	}

	@Override
	public Optional<Page> queryPage(Integer id) {
		return Transactions.executeInReadOnlyTransaction(platformTransactionManager, status -> {
			return Optional.ofNullable(pageDao.selectById(id));
		});
	}

	@Override
	public PageResult<Page> queryPage(TemplatePageQueryParam param) {
		return Transactions.executeInReadOnlyTransaction(platformTransactionManager, status -> {
			param.setPageSize(configService.getGlobalConfig().getPagePageSize());
			int count = pageDao.selectCount(param);
			List<Page> datas = pageDao.selectPage(param);
			return new PageResult<>(param, count, datas);
		});
	}

	@Override
	public synchronized void deletePage(Integer id) throws LogicException {
		executeInTransaction(() -> {
			Page db = pageDao.selectById(id);
			if (db == null) {
				throw new LogicException(USER_PAGE_NOT_EXISTS);
			}
			pageDao.deleteById(id);
			String templateName = db.getTemplateName();
			evitPageCache(templateName);
			this.applicationEventPublisher.publishEvent(new PageEvent(this, EventType.DELETE, db));
			new PageRequestMappingRegisterHelper().unregisterPage(db);
		});
	}

	@Override
	public List<String> queryDataTags() {
		return processors.stream().map(processor -> processor.getName()).collect(Collectors.toList());
	}

	@Override
	public synchronized void buildTpl(Page page) throws LogicException {
		executeInTransaction(() -> {
			enablePageAliasNotContainsSpace(page.getAlias());
			final PageRequestMappingRegisterHelper helper = new PageRequestMappingRegisterHelper();
			checkSpace(page);
			String alias = page.getAlias();
			page.setCreateDate(Timestamp.valueOf(LocalDateTime.now()));
			boolean update = page.hasId();
			if (update) {
				Page db = pageDao.selectById(page.getId());
				if (db == null) {
					throw new LogicException(USER_PAGE_NOT_EXISTS);
				}
				// 检查
				Page aliasPage = pageDao.selectBySpaceAndAlias(page.getSpace(), alias);
				if (aliasPage != null && !aliasPage.getId().equals(page.getId())) {
					throw new LogicException("page.user.aliasExists", "别名" + alias + "已经存在", alias);
				}
				pageDao.update(page);

				evitPageCache(db);

				// 解除以前的mapping
				helper.unregisterPage(db);
			} else {
				// 检查
				Page aliasPage = pageDao.selectBySpaceAndAlias(page.getSpace(), alias);
				if (aliasPage != null) {
					throw new LogicException("page.user.aliasExists", "别名" + alias + "已经存在", alias);
				}
				pageDao.insert(page);

				evitPageCache(page);
			}

			// 注册现在的页面
			helper.registerPage(page);

			EventType type = update ? EventType.UPDATE : EventType.INSERT;
			this.applicationEventPublisher.publishEvent(new PageEvent(this, type, page));
		});
	}

	/**
	 * 确保用户自定页面路径中不包含space/{alias}或者类似space的信息
	 * 
	 * @param alias
	 * @throws LogicException
	 */
	private void enablePageAliasNotContainsSpace(String alias) throws LogicException {
		if (UrlUtils.match("space/*/**", alias)) {
			throw new LogicException("page.alias.containsSpace", "路径中不能包含space信息");
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
	public Optional<Template> queryTemplate(String templateName) {
		return Transactions.executeInReadOnlyTransaction(platformTransactionManager, status -> {
			for (TemplateProcessor processor : templateProcessors) {
				if (processor.canProcess(templateName)) {
					return Optional.ofNullable(processor.getTemplate(templateName));
				}
			}
			throw new SystemException("无法处理模板名：" + templateName + "对应的模板");
		});
	}

	@Override
	public List<ExportPage> exportPage(Integer spaceId) throws LogicException {
		DefaultTransactionDefinition td = new DefaultTransactionDefinition();
		td.setReadOnly(true);
		TransactionStatus status = platformTransactionManager.getTransaction(td);
		try {
			Space space = spaceCache.checkSpace(spaceId);
			List<ExportPage> exportPages = new ArrayList<>();
			// User
			for (Page page : pageDao.selectBySpace(space)) {
				exportPages.add(export(page));
			}
			return exportPages;
		} catch (LogicException | RuntimeException | Error e) {
			status.setRollbackOnly();
			throw e;
		} finally {
			platformTransactionManager.commit(status);
		}
	}

	@Override
	public synchronized void compareTemplate(String templateName, Template template, Consumer<Boolean> consumer) {
		Transactions.executeInReadOnlyTransaction(platformTransactionManager, status -> {
			Optional<Template> current = queryTemplate(templateName);
			boolean equalsTo = current.isPresent() && template != null && current.get().equalsTo(template);
			consumer.accept(equalsTo);
		});
	}

	@Override
	public synchronized List<ImportRecord> importPage(Integer spaceId, List<ExportPage> exportPages) {
		if (CollectionUtils.isEmpty(exportPages)) {
			return new ArrayList<>();
		}
		DefaultTransactionDefinition td = new DefaultTransactionDefinition();
		td.setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);
		td.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		TransactionStatus ts = platformTransactionManager.getTransaction(td);
		List<ImportRecord> records = new ArrayList<>();
		PageRequestMappingRegisterHelper helper = new PageRequestMappingRegisterHelper();
		try {
			Space space = null;
			try {
				space = spaceCache.checkSpace(spaceId);
			} catch (LogicException e) {
				ts.setRollbackOnly();
				return Arrays.asList(new ImportRecord(false, e.getLogicMessage()));
			}
			Set<String> pageEvitKeySet = new HashSet<>();
			Set<String> fragmentEvitKeySet = new HashSet<>();

			List<Page> pages = exportPages.stream().map(ExportPage::getPage).collect(Collectors.toList());
			List<Fragment> fragments = exportPages.stream().flatMap(ep -> ep.getFragments().stream()).distinct()
					.collect(Collectors.toList());

			for (Page page : pages) {
				try {
					enablePageAliasNotContainsSpace(page.getAlias());
				} catch (LogicException e) {
					records.add(new ImportRecord(true, e.getLogicMessage()));
					ts.setRollbackOnly();
					return records;
				}
				page.setSpace(space);
				String templateName = page.getTemplateName();
				Optional<Page> optional = queryPageWithTemplateName(templateName);
				if (!optional.isPresent()) {
					// 如果插入用户页面
					page.setCreateDate(Timestamp.valueOf(LocalDateTime.now()));
					page.setDescription("");
					page.setAllowComment(false);

					pageDao.insert(page);

					try {
						helper.registerPage(page);
					} catch (LogicException ex) {
						records.add(new ImportRecord(true, ((LogicException) ex).getLogicMessage()));
						ts.setRollbackOnly();
						return records;
					}

					records.add(new ImportRecord(true, new Message("import.insert.page.success",
							"插入页面" + page.getName() + "[" + page.getAlias() + "]成功", page.getName(), page.getAlias())));
					pageEvitKeySet.add(templateName);
				} else {
					Page current = optional.get();
					// 如果页面内容发生了改变
					if (!checkChangeWhenImport(current, page)) {
						current.setTpl(page.getTpl());
						pageDao.update(current);
						helper.unregisterPage(current);
						try {
							helper.registerPage(current);
						} catch (LogicException ex) {
							records.add(new ImportRecord(true, ((LogicException) ex).getLogicMessage()));
							ts.setRollbackOnly();
							return records;
						}
						records.add(new ImportRecord(true,
								new Message("import.update.page.success",
										"更新页面" + page.getName() + "[" + page.getAlias() + "]成功", page.getName(),
										page.getAlias())));

						pageEvitKeySet.add(templateName);
					} else {
						records.add(new ImportRecord(true, new Message("import.page.nochange",

								"页面" + page.getName() + "[" + page.getAlias() + "]内容没有发生变化，无需更新", page.getName(),
								page.getAlias())));
					}
				}
			}

			for (Fragment fragment : fragments) {
				String fragmentName = fragment.getName();
				fragment.setSpace(space);
				// 查询当前的fragment
				Optional<Fragment> optionalFragment = queryFragmentWithTemplateName(fragment.getTemplateName());
				if (!optionalFragment.isPresent()) {
					// 插入fragment
					insertFragmentWhenImport(fragment, records);
					fragmentEvitKeySet.add(fragmentName);
				} else {
					Fragment currentFragment = optionalFragment.get();
					if (currentFragment.getTpl().equals(fragment.getTpl())) {
						records.add(new ImportRecord(true, new Message("import.fragment.nochange",
								"模板片段" + fragmentName + "内容没有发生变化，无需更新", fragmentName)));
					} else {
						// 如果用户存在同名的fragment，但是是global的，则插入space级别的
						if (currentFragment.isGlobal()) {
							insertFragmentWhenImport(fragment, records);
						} else {
							currentFragment.setTpl(fragment.getTpl());
							fragmentDao.update(currentFragment);
							records.add(new ImportRecord(true, new Message("import.update.fragment.success",
									"模板片段" + fragmentName + "更新成功", fragmentName)));
						}
						fragmentEvitKeySet.add(fragmentName);
					}
				}
			}

			evitPageCache(pageEvitKeySet.stream().toArray(i -> new String[i]));
			evitFragmentCache(fragmentEvitKeySet.stream().toArray(i -> new String[i]));
			return records;
		} catch (Throwable e) {
			LOGGER.error(e.getMessage(), e);
			ts.setRollbackOnly();
			records.add(new ImportRecord(true, Constants.SYSTEM_ERROR));
			return records;
		} finally {
			platformTransactionManager.commit(ts);
		}
	}

	@Override
	public PathTemplateService getPathTemplateService() throws LogicException {
		if (!enablePathTemplate) {
			throw new LogicException("pathTemplate.service.disable", "物理文件模板服务不可用");
		}
		return pathTemplateService;
	}

	@Override
	public PreviewService getPreviewService() {
		return previewService;
	}

	/**
	 * 导入页面时判断页面是否需要更新
	 * 
	 * @param old
	 * @param current
	 * @return
	 */
	private boolean checkChangeWhenImport(Page old, Page current) {
		if (current.getTpl().equals(old.getTpl())) {
			return true;
		}
		return false;
	}

	/**
	 * 导入时候插入fragment
	 * 
	 * @param toImport
	 * @param records
	 */
	private void insertFragmentWhenImport(Fragment toImport, List<ImportRecord> records) {
		Fragment fragment = new Fragment();
		fragment.setCreateDate(Timestamp.valueOf(LocalDateTime.now()));
		fragment.setDescription("");
		fragment.setGlobal(false);
		fragment.setName(toImport.getName());
		fragment.setTpl(toImport.getTpl());
		fragment.setSpace(toImport.getSpace());
		fragmentDao.insert(fragment);
		records.add(new ImportRecord(true,
				new Message("import.insert.tpl.success", "模板" + toImport.getName() + "插入成功", toImport.getName())));
	}

	@EventListener
	public void handleContextRefreshEvent(ContextRefreshedEvent evt) throws Exception {
		if (evt.getApplicationContext().getParent() != null) {

			Transactions.executeInReadOnlyTransaction(platformTransactionManager, status -> {
				PageRequestMappingRegisterHelper helper = new PageRequestMappingRegisterHelper();
				List<Page> allPage = pageDao.selectAll();
				for (Page page : allPage) {
					try {
						helper.registerPage(page);
					} catch (LogicException e) {
						throw new SystemException(e.getLogicMessage().getCodes()[0]);
					}
				}
			});

			if (enablePathTemplate) {
				pathTemplateService.loadPathTemplateFile("");
			}
		}

	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		this.applicationEventPublisher = applicationEventPublisher;
	}

	@Override
	public void afterPropertiesSet() throws Exception {

		templateMappingRegister = new TemplateMappingRegister((TemplateRequestMappingHandlerMapping) requestMapping);

		initSystemTemplates();

		if (pathTemplateRoot != null) {
			FileUtils.forceMkdir(pathTemplateRoot);

			pathTemplateService = new PathTemplateServiceImpl();
			LOGGER.debug("开启了物理文件模板服务");
			enablePathTemplate = true;
		}

		executeInTransaction(() -> {
			Timestamp now = Timestamp.valueOf(Times.now());
			for (Fragment _fragment : fragments) {
				Fragment fragment = fragmentDao.selectGlobalByName(_fragment.getName());
				if (fragment == null) {
					fragment = new Fragment();
					fragment.setCallable(_fragment.isCallable());
					fragment.setCreateDate(now);
					fragment.setDescription("");
					fragment.setGlobal(true);
					fragment.setName(_fragment.getName());
					fragment.setSpace(null);
					fragment.setTpl(_fragment.getTpl());
					fragmentDao.insert(fragment);
				}
			}
		});

		// System Template Processor
		this.templateProcessors.add(new TemplateProcessor() {

			@Override
			public Template getTemplate(String templateName) {
				String[] array = templateName.split(Template.SPLITER);
				String path;
				if (array.length == 3) {
					path = array[2];
				} else if (array.length == 2) {
					path = "";
				} else {
					throw new SystemException("无法从" + templateName + "中获取路径");
				}
				Template template = defaultTemplates.get(path);
				if (template != null) {
					template = template.cloneTemplate();
				}
				return template;
			}

			@Override
			public boolean canProcess(String templateName) {
				return SystemTemplate.isSystemTemplate(templateName);
			}
		});

		// Page Template Processor
		this.templateProcessors.add(new TemplateProcessor() {

			@Override
			public Template getTemplate(String templateName) {
				return queryPageWithTemplateName(templateName).orElse(null);
			}

			@Override
			public boolean canProcess(String templateName) {
				return Page.isPageTemplate(templateName);
			}
		});

		// Fragment Template Processor
		this.templateProcessors.add(new TemplateProcessor() {

			@Override
			public Template getTemplate(String templateName) {
				return queryFragmentWithTemplateName(templateName).orElse(null);
			}

			@Override
			public boolean canProcess(String templateName) {
				return Fragment.isFragmentTemplate(templateName);
			}
		});

		// PathTemplate Processor
		if (enablePathTemplate) {
			this.templateProcessors.add(new TemplateProcessor() {

				@Override
				public Template getTemplate(String templateName) {
					return pathTemplateService.getPathTemplate(templateName).orElse(null);
				}

				@Override
				public boolean canProcess(String templateName) {
					return PathTemplate.isPathTemplate(templateName);
				}
			});
		}

		// 预览文件模板服务
		this.previewService = new PreviewServiceImpl();

		this.templateProcessors.add(new TemplateProcessor() {

			@Override
			public Template getTemplate(String templateName) {
				return previewService.getTemplate(templateName).orElse(null);
			}

			@Override
			public boolean canProcess(String templateName) {
				return Template.isPreviewTemplate(templateName);
			}
		});
	}

	/**
	 * 初始化系统默认模板，这些模板都能被删除
	 * 
	 * @throws Exception
	 */
	private void initSystemTemplates() throws Exception {
		defaultTemplates = new HashMap<>();
		defaultTemplates.put("", new SystemTemplate("", "resources/page/PAGE_INDEX.html"));
		defaultTemplates.put("login", new SystemTemplate("login", "resources/page/LOGIN.html"));
		defaultTemplates.put("space/{alias}", new SystemTemplate("space/{alias}", "resources/page/PAGE_INDEX.html"));
		defaultTemplates.put("unlock", new SystemTemplate("unlock", "resources/page/PAGE_LOCK.html"));
		defaultTemplates.put("space/{alias}/unlock",
				new SystemTemplate("space/{alias}/unlock", "resources/page/PAGE_LOCK.html"));

		defaultTemplates.put("error", new SystemTemplate("error", "resources/page/PAGE_ERROR.html"));
		defaultTemplates.put("space/{alias}/error",
				new SystemTemplate("space/{alias}/error", "resources/page/PAGE_ERROR.html"));
		defaultTemplates.put("space/{alias}/article/{idOrAlias}",
				new SystemTemplate("space/{alias}/article/{idOrAlias}", "resources/page/PAGE_ARTICLE_DETAIL.html"));

		long stamp = templateMappingRegister.lockWrite();
		try {
			for (Map.Entry<String, SystemTemplate> it : defaultTemplates.entrySet()) {
				this.templateMappingRegister.registerTemplateMapping(it.getValue().getTemplateName(), it.getKey());
			}
		} finally {
			templateMappingRegister.unlockWrite(stamp);
		}
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

	private void checkSpace(Fragment fragment) throws LogicException {
		Space space = fragment.getSpace();
		if (space != null) {
			fragment.setSpace(spaceCache.checkSpace(space.getId()));
		}
	}

	private void evitPageCache(String... templateNames) {
		Transactions.afterCommit(() -> {
			this.applicationEventPublisher.publishEvent(new TemplateEvitEvent(this, templateNames));
		});
	}

	private void evitPageCache(Page... pages) {
		evitPageCache(Arrays.stream(pages).map(Page::getTemplateName).toArray(i -> new String[i]));
	}

	private void evitFragmentCache(String... names) {
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

	private Optional<Fragment> queryFragmentWithTemplateName(String templateName) {
		String[] array = templateName.split(Template.SPLITER);
		String name;
		Space space = null;
		if (array.length == 3) {
			name = array[2];
		} else if (array.length == 4) {
			name = array[2];
			space = new Space(Integer.parseInt(array[3]));
		} else {
			throw new SystemException(templateName + "无法转化为Fragment");
		}
		Fragment fragment = fragmentDao.selectBySpaceAndName(space, name);
		if (fragment == null) { // 查找全局
			fragment = fragmentDao.selectGlobalByName(name);
		}
		return Optional.ofNullable(fragment);
	}

	private Optional<Page> queryPageWithTemplateName(String templateName) {
		Page page = null;
		String[] array = templateName.split(Template.SPLITER);
		if (array.length == 2) {
			page = pageDao.selectBySpaceAndAlias(null, "");
		} else if (array.length == 3) {
			page = pageDao.selectBySpaceAndAlias(null, array[2]);
		} else if (array.length == 4) {
			page = pageDao.selectBySpaceAndAlias(new Space(Integer.parseInt(array[3])), array[2]);
		} else {
			throw new SystemException(templateName + "无法转化为用户自定义页面");
		}
		return Optional.ofNullable(page);
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
			Optional<Fragment> optional = queryFragmentWithTemplateName(new Fragment(name, space).getTemplateName());
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

	private final class PathTemplateServiceImpl implements PathTemplateService {

		private static final String PATH_TEMPLATE_SUFFIX = ".html";
		private static final String PATH_TEMPLATE_REG_SUFFIX = ".reg.html";
		private static final String PUBLIC_FRAGMENT_TEMPLATE_SUFFIX = ".pub.html";
		private static final String PREVIEW_SUFFIX = ".preview.html";

		private final Map<String, PathTemplate> pathTemplates = new ConcurrentHashMap<>();
		private final Map<String, PathTemplate> publicFragments = new ConcurrentHashMap<>();

		@Override
		public List<PathTemplate> queryPathTemplates(String pattern) {
			if (Validators.isEmptyOrNull(pattern, true)) {
				List<PathTemplate> templates = new ArrayList<>(pathTemplates.values());
				templates.addAll(publicFragments.values());
				return Collections.unmodifiableList(templates);
			}

			PatternFilter filter = new PatternFilter(pattern);
			List<PathTemplate> templates = new ArrayList<>(
					pathTemplates.values().parallelStream().filter(filter).collect(Collectors.toList()));
			templates.addAll(publicFragments.values().parallelStream().filter(filter).collect(Collectors.toList()));
			return templates;
		}

		@Override
		public PathTemplate registerPreview(String path) throws LogicException {
			String cleanPath = FileUtils.cleanPath(path);
			Path lookupPath = pathTemplateRoot.resolve(cleanPath);
			if (!Files.exists(lookupPath)) {
				throw new LogicException("pathTemplate.preview.sourceNotExists", "文件不存在");
			}
			if (!FileUtils.isSub(lookupPath, pathTemplateRoot)) {
				throw new LogicException("pathTemplate.preview.notInRoot", "文件不在模板主目录中");
			}
			if (!Files.isRegularFile(lookupPath)) {
				throw new LogicException("pathTemplate.preview.notFile", "文件夹不能被预览");
			}
			String relativePath = getPathTemplatePath(lookupPath);
			PathTemplate template = pathTemplates.get(relativePath);
			if (template == null) {
				throw new LogicException("pathTemplate.preview.notAssociate", "文件没有被注册");
			}
			if (!template.isRegistrable()) {
				throw new LogicException("pathTemplate.preview.notRegistrable", "模板片段不能被预览");
			}
			// 查找preview
			Path previewPath = pathTemplateRoot.resolve(relativePath + PREVIEW_SUFFIX);
			if (!Files.exists(previewPath)) {
				throw new LogicException("pathTemplate.preview.previewNotExists", "预览文件不存在");
			}
			if (!Files.isRegularFile(previewPath)) {
				throw new LogicException("pathTemplate.preview.previewNotFile", "预览文件不是一个文件");
			}
			if (!Files.isReadable(previewPath)) {
				throw new LogicException("pathTemplate.preview.previewNotReadable", "预览文件不可读");
			}
			PathTemplate preview = new PathTemplate(previewPath, true, relativePath);
			previewService.registerPreview(relativePath, preview);
			return preview;
		}

		@Override
		public Optional<PathTemplate> getPathTemplate(String templateName) {
			if (!PathTemplate.isPathTemplate(templateName)) {
				return Optional.empty();
			}
			String path;
			String spaceAlias = null;
			// Template%Path%path
			// Template%Path%
			// Template%Path%%space
			// Template%Path%path%space
			String[] array = templateName.split(Template.SPLITER);
			if (array.length == 2) {
				path = "";
			} else if (array.length == 3) {
				path = array[2];
			} else if (array.length == 4) {
				path = array[2];
				spaceAlias = array[3];
			} else {
				throw new SystemException("无法从" + templateName + "中获取路径");
			}

			PathTemplate template = null;
			path = FileUtils.cleanPath(path);
			// 从空间中寻找
			if (spaceAlias == null) {
				// 如果是默认空间，无法加载其他空间的PathTemplate
				if (!UrlUtils.match("space/*/**", path)) {
					template = pathTemplates.get(path);
				}
			} else {
				template = pathTemplates.get("space/" + spaceAlias + (path.isEmpty() ? "" : "/" + path));
			}
			// 从全局fragment中寻找
			if (template == null) {
				template = publicFragments.get(path);
			}
			if (template != null) {
				template = new PathTemplate(template);
			}
			return Optional.ofNullable(template);
		}

		@Override
		public List<PathTemplateLoadRecord> loadPathTemplateFile(String path) {
			// 锁住Outer class，为了保证queryTemplate的时候没有其他写线程修改数据
			synchronized (TemplateServiceImpl.this) {
				// 获取RequestMapping的锁
				long stamp = templateMappingRegister.lockWrite();
				try {
					// 定位需要载入的目录
					Path loadPath = pathTemplateRoot.resolve(FileUtils.cleanPath(path));
					if (!Files.exists(loadPath)) {
						LOGGER.debug("文件" + loadPath + "不存在");
						return Arrays.asList(new PathTemplateLoadRecord(null, false,
								new Message("pathTemplate.load.path.notExists", "路径不存在")));
					}
					if (!FileUtils.isSub(loadPath, pathTemplateRoot)) {
						LOGGER.debug("文件" + loadPath + "不存在于模板主目录中");
						return Arrays.asList(new PathTemplateLoadRecord(null, false,
								new Message("pathTemplate.load.path.notInRoot", "文件不存在于模板主目录中")));
					}

					List<PathTemplateLoadRecord> records = new ArrayList<>();

					if (Files.isRegularFile(loadPath)) {
						if (!isPreview(loadPath)) {
							records.add(this.loadPathTemplateFile(loadPath));
						}
					} else {
						// 查找被删除(期望解除注册)的文件
						for (PathTemplate pathTemplate : pathTemplates.values()) {
							Path associate = pathTemplate.getAssociate();
							if (FileUtils.isSub(associate, loadPath) && !Files.exists(associate)) {
								String relativePath = getPathTemplatePath(associate);
								if (pathTemplate.isRegistrable()) {
									// 如果是可注册的，删除mapping
									templateMappingRegister.unregisterTemplateMapping(relativePath);
								}
								pathTemplates.remove(relativePath);
								applicationEventPublisher
										.publishEvent(new TemplateEvitEvent(this, pathTemplate.getTemplateName()));
								LOGGER.debug("文件" + associate + "不存在，删除");
								records.add(new PathTemplateLoadRecord(relativePath, true,
										new Message("pathTemplate.load.removeSuccess", "删除成功")));
							}
						}
						// 从公共fragment中删除
						for (PathTemplate publicFragment : publicFragments.values()) {
							Path associate = publicFragment.getAssociate();
							if (FileUtils.isSub(associate, loadPath) && !Files.exists(associate)) {
								String relativePath = getPathTemplatePath(associate);
								publicFragments.remove(relativePath);
								applicationEventPublisher
										.publishEvent(new TemplateEvitEvent(this, publicFragment.getTemplateName()));
								LOGGER.debug("文件" + associate + "不存在，删除");
								records.add(new PathTemplateLoadRecord(relativePath, true,
										new Message("pathTemplate.load.removeSuccess", "删除成功")));
							}
						}

						try {
							// 寻找文件夹下所有符合条件的路径
							records.addAll(
									Files.walk(loadPath, PageValidator.MAX_ALIAS_DEPTH).filter(Files::isRegularFile)
											.filter(this::isPathTemplate).filter(p -> !isPreview(p))
											.map(this::loadPathTemplateFile).collect(Collectors.toList()));
						} catch (IOException e) {
							throw new SystemException(e.getMessage(), e);
						}
					}

					return records;
				} finally {
					templateMappingRegister.unlockWrite(stamp);
				}
			}
		}

		// 载入一个文件
		private PathTemplateLoadRecord loadPathTemplateFile(Path file) {
			String relativePath = getPathTemplatePath(file);

			if (pathTemplates.containsKey(relativePath)) {
				PathTemplate exists = pathTemplates.get(relativePath);
				if (file.getFileName().toString().equals(exists.getAssociate().getFileName().toString())) {
					// 如果是同一个文件
					// 刷新缓存
					String templateName = exists.getTemplateName();
					applicationEventPublisher.publishEvent(new TemplateEvitEvent(this, templateName));
					// 如果是可注册的，尝试注册，因为当容器重启时路径会丢失
					if (isRegPathTemplate(file)) {
						try {
							templateMappingRegister.registerTemplateMapping(templateName, relativePath);
						} catch (LogicException e) {
							// 忽略这个异常，已经被注册了
						}
					}
					LOGGER.debug("文件" + file + "载入成功");
					return new PathTemplateLoadRecord(relativePath, true,
							new Message("pathTemplate.load.loadSuccess", "载入成功"));
				} else {
					// 不是同一个文件，却有相同的relativePath
					// /dir/a.reg.html
					// /dir/a.html
					LOGGER.debug("文件:" + file + "已经存在");
					return new PathTemplateLoadRecord(relativePath, true,
							new Message("pathTemplate.load.exists", "文件已经存在"));
				}
			}

			// 公共fragments中已经存在，此时不可能指向不同的文件，刷新缓存
			if (publicFragments.containsKey(relativePath)) {
				String templateName = publicFragments.get(relativePath).getTemplateName();
				applicationEventPublisher.publishEvent(new TemplateEvitEvent(this, templateName));
				LOGGER.debug("文件" + file + "载入成功");
				return new PathTemplateLoadRecord(relativePath, true,
						new Message("pathTemplate.load.loadSuccess", "载入成功"));
			}

			if (!Files.exists(file)) {
				LOGGER.debug("文件" + file + "不存在");
				return new PathTemplateLoadRecord(relativePath, false,
						new Message("pathTemplate.load.path.notExists", "路径不存在"));
			}
			if (!Files.isReadable(file)) {
				LOGGER.debug("文件" + file + "不可读");
				return new PathTemplateLoadRecord(relativePath, false,
						new Message("pathTemplate.load.path.notReadable", "路径不可读"));
			}
			if (isRegPathTemplate(file)) {
				// 验证注册路径的正确性
				Errors errors = new MapBindingResult(new HashMap<>(), "pathTemplate");
				relativePath = PageValidator.validateAlias(relativePath, errors);
				if (errors.hasErrors()) {
					ObjectError first = errors.getAllErrors().get(0);
					LOGGER.debug("文件" + file + "，对应的映射路径:" + relativePath + "校验失败");
					return new PathTemplateLoadRecord(relativePath, false,
							new Message(first.getCode(), first.getDefaultMessage(), first.getArguments()));
				}

				PathTemplate pathTemplate = new PathTemplate(file, true, relativePath);
				String templateName = pathTemplate.getTemplateName();
				try {
					templateMappingRegister.registerTemplateMapping(templateName, relativePath);
					LOGGER.debug("文件" + file + "，对应的映射路径:" + relativePath + "，注册成功");
					applicationEventPublisher.publishEvent(new TemplateEvitEvent(this, templateName));
				} catch (LogicException e) {
					LOGGER.debug("文件" + file + "，对应的映射路径:" + relativePath + "，注册失败，路径已经存在");
					return new PathTemplateLoadRecord(relativePath, false, e.getLogicMessage());
				}

				pathTemplates.put(relativePath, pathTemplate);
			} else {
				// 不是可注册的
				// fragment
				// 如果是全局fragment
				boolean isPublic = isPublicFragment(file);
				if (isPublic) {
					// 全局fragment不能在space/x/文件夹下
					if (UrlUtils.match("space/*/**", relativePath)) {
						LOGGER.debug("全局fragment:" + relativePath + "不能在space/xx/文件夹下");
						return new PathTemplateLoadRecord(relativePath, false,
								new Message("pathTemplate.load.path.publicFragmentInSpace",
										"路径:" + relativePath + "被标记为全局fragment，该路径不能位于space文件夹下", relativePath));
					}
				}

				// 校验fragment name
				String name = FileUtils.getNameWithoutExtension(relativePath);
				if (!name.matches(FragmentValidator.NAME_PATTERN)) {
					return new PathTemplateLoadRecord(relativePath, false,
							new Message("pathTemplate.load.fragment.invalidName", "无效的名称:" + name, name));
				}
				PathTemplate pathTemplate = new PathTemplate(file, false, relativePath);

				String templateName = pathTemplate.getTemplateName();
				applicationEventPublisher.publishEvent(new TemplateEvitEvent(this, templateName));

				if (isPublic) {
					publicFragments.put(relativePath, pathTemplate);
				} else {
					pathTemplates.put(relativePath, pathTemplate);
				}
			}
			LOGGER.debug("文件" + file + "载入成功");
			return new PathTemplateLoadRecord(relativePath, true, new Message("pathTemplate.load.loadSuccess", "载入成功"));
		}

		private boolean isPublicFragment(Path path) {
			return path.toString().endsWith(PUBLIC_FRAGMENT_TEMPLATE_SUFFIX);
		}

		private boolean isRegPathTemplate(Path path) {
			return path.toString().endsWith(PATH_TEMPLATE_REG_SUFFIX);
		}

		private boolean isPreview(Path path) {
			return path.toString().endsWith(PREVIEW_SUFFIX);
		}

		private boolean isPathTemplate(Path path) {
			return path.toString().endsWith(PATH_TEMPLATE_SUFFIX);
		}

		private String getPathTemplatePath(Path path) {
			String relativizePath = pathTemplateRoot.relativize(path).toString();
			if (isRegPathTemplate(path)) {
				relativizePath = relativizePath.substring(0,
						relativizePath.length() - PATH_TEMPLATE_REG_SUFFIX.length());
			} else if (isPublicFragment(path)) {
				relativizePath = relativizePath.substring(0,
						relativizePath.length() - PUBLIC_FRAGMENT_TEMPLATE_SUFFIX.length());
			} else {
				relativizePath = relativizePath.substring(0, relativizePath.length() - PATH_TEMPLATE_SUFFIX.length());
			}
			return FileUtils.cleanPath(relativizePath);
		}

		private final class PatternFilter implements Predicate<PathTemplate> {

			private final String pattern;

			public PatternFilter(String pattern) {
				super();
				this.pattern = pattern;
			}

			@Override
			public boolean test(PathTemplate t) {
				return t.getRelativePath().matches(pattern);
			}
		}
	}

	private final class PreviewServiceImpl implements PreviewService {

		private Map<String, Template> previewMap = new HashMap<>();
		private ReadWriteLock lock = new ReentrantReadWriteLock();

		@Override
		public void registerPreview(String path, Template template) throws LogicException {
			lock.writeLock().lock();
			try {
				long stamp = templateMappingRegister.lockWrite();
				try {
					String cleanPath = FileUtils.cleanPath(path);
					templateMappingRegister.registerPreviewMapping(template.getTemplateName(), cleanPath);
					previewMap.put(cleanPath, template);
				} finally {
					templateMappingRegister.unlockWrite(stamp);
				}
			} finally {
				lock.writeLock().unlock();
			}
		}

		@Override
		public void clearPreview() {
			lock.writeLock().lock();
			try {
				previewMap.clear();
				long stamp = templateMappingRegister.lockWrite();
				try {
					for (String path : previewMap.keySet()) {
						templateMappingRegister.unregisterPreviewMapping(path);
					}
				} finally {
					templateMappingRegister.unlockWrite(stamp);
				}
			} finally {
				lock.writeLock().unlock();
			}
		}

		@Override
		public Optional<Template> getTemplate(String templateName) {
			if (!Template.isPreviewTemplate(templateName)) {
				return Optional.empty();
			}
			lock.readLock().lock();
			try {
				if (previewMap.isEmpty()) {
					return Optional.empty();
				}
				String path = templateName.substring(Template.TEMPLATE_PREVIEW_PREFIX.length());
				return Optional.ofNullable(previewMap.get(path));
			} finally {
				lock.readLock().unlock();
			}
		}

		/**
		 * <p>
		 * 获取最匹配的preview路径<br>
		 * 如果注册了space/test/{test}路径，那么访问space/test/apk将会匹配到该路径<br>
		 * 但是如果space/test/apk已经存在，仍然不会进入space/test/apk这个mapping，而是进入space/test/{test}这个mapping<br>
		 * 为了防止这种情况的发生，将当前被匹配到的path用于比对，如果preview中的path级别比当前path高，则用preview中的，否则用当前路径的<br>
		 * </p>
		 * 
		 * @param currentPath
		 * @param templateName
		 * @return
		 */
		Optional<String> getBestMatchTemplateName(String currentPath, String templateName) {
			if (!Template.isPreviewTemplate(templateName)) {
				return Optional.empty();
			}
			lock.readLock().lock();
			try {
				if (previewMap.isEmpty()) {
					return Optional.empty();
				}
				// 如果直接匹配成功
				String path = templateName.substring(Template.TEMPLATE_PREVIEW_PREFIX.length());
				Template template = previewMap.get(path);
				if (template == null) {
					// 没有直接匹配的path，只能遍历全部path，寻找最合适的path
					Set<String> pathSet = new HashSet<>(previewMap.keySet());
					if (currentPath != null) {
						pathSet.add(currentPath);
					}
					PatternsRequestCondition condition = new PatternsRequestCondition(
							pathSet.toArray(new String[pathSet.size()]));
					List<String> matches = condition.getMatchingPatterns("/" + path);
					if (!matches.isEmpty()) {
						String bestMatch = matches.get(0).substring(1);
						template = previewMap.get(bestMatch);
						if (template != null) {
							return Optional.of(Template.TEMPLATE_PREVIEW_PREFIX + bestMatch);
						}
					}
					return Optional.empty();
				} else {
					return Optional.of(Template.TEMPLATE_PREVIEW_PREFIX + path);
				}
			} finally {
				lock.readLock().unlock();
			}
		}
	}

	private void executeInTransaction(LogicExecutor executor) throws LogicException {
		DefaultTransactionDefinition td = new DefaultTransactionDefinition();
		TransactionStatus status = platformTransactionManager.getTransaction(td);
		try {
			executor.execute();
		} catch (Throwable e) {
			status.setRollbackOnly();
			throw e;
		} finally {
			platformTransactionManager.commit(status);
		}
	}

	private static String readClassPathResourceToString(String classPath) {
		try {
			return Resources.readResourceToString(new ClassPathResource(classPath));
		} catch (IOException e) {
			throw new SystemException(e.getMessage(), e);
		}
	}

	private interface TemplateProcessor {
		/**
		 * 是否能够处理该模板
		 * 
		 * @param templateName
		 * @return
		 */
		boolean canProcess(String templateName);

		/**
		 * 根据模板名查询模板
		 * 
		 * @param templateName
		 *            模板名
		 * @return 模板，如果不存在，返回null
		 */
		Template getTemplate(String template);
	}

	/**
	 * 用来注册template mapping
	 * <p>
	 * <b>这个类的注册、强制注册和解除注册都不是线程安全的，可以通过lockWrite和unlockWrite来保证线程安全</b>
	 * </p>
	 * <p>
	 * <b>lockWrite和unlockWrite同时会阻塞前端页面的渲染</b>
	 * </p>
	 * 
	 * @author mhlx
	 *
	 */
	private final class TemplateMappingRegister {

		private final Method method;
		private final TemplateRequestMappingHandlerMapping requestMappingHandlerMapping;
		private final PatternsRequestCondition condition;

		private TemplateMappingRegister(TemplateRequestMappingHandlerMapping requestMappingHandlerMapping)
				throws Exception {
			super();
			this.requestMappingHandlerMapping = requestMappingHandlerMapping;
			method = TemplateController.class.getMethod("handleRequest", HttpServletRequest.class);
			Set<String> patternSet = new HashSet<>();

			// 遍历所有的系统默认路径
			for (Map.Entry<RequestMappingInfo, HandlerMethod> it : requestMappingHandlerMapping
					.getHandlerMethodsUnsafe().entrySet()) {
				RequestMappingInfo info = it.getKey();
				HandlerMethod method = it.getValue();
				PatternsRequestCondition condition = info.getPatternsCondition();
				if (!(method.getBean() instanceof TemplateController)) {
					RequestMethodsRequestCondition methodsRequestCondition = info.getMethodsCondition();
					if (methodsRequestCondition.isEmpty()
							|| methodsRequestCondition.getMethods().contains(RequestMethod.GET)) {
						patternSet.addAll(condition.getPatterns());
					}
				}
			}
			condition = new PatternsRequestCondition(patternSet.toArray(new String[patternSet.size()]));
		}

		public long lockWrite() {
			return requestMappingHandlerMapping.lockWrite();
		}

		public void unlockWrite(long stamp) {
			this.requestMappingHandlerMapping.unlockWrite(stamp);
		}

		/**
		 * 解除注册预览mapping
		 * 
		 * @param path
		 *            路径
		 */
		public void unregisterPreviewMapping(String path) {
			path = FileUtils.cleanPath(path);
			if (isKeyPath(path)) {
				return;
			}
			RequestMappingInfo mapping = getMethodMapping(path);
			HandlerMethod handlerMethod = requestMappingHandlerMapping.getHandlerMethodsUnsafe().get(mapping);
			if (handlerMethod != null) {
				TemplateController templateController = (TemplateController) handlerMethod.getBean();
				if (Template.isPreviewTemplate(templateController.templateName)) {
					// 此时可以解除
					requestMappingHandlerMapping.unregisterMappingUnsafe(mapping);
				}
			}
		}

		/**
		 * 注册预览mapping
		 * 
		 * @param templateName
		 *            模板名
		 * @param path
		 *            路径
		 */
		public void registerPreviewMapping(String templateName, String path) throws LogicException {
			path = FileUtils.cleanPath(path);
			// 判断是否是系统保留路径
			if (isKeyPath(path)) {
				throw new LogicException("requestMapping.preview.keyPath", "路径" + path + "是系统保留路径，无法被预览");
			}
			// 查找是否已经存在可以被映射的RequestMapping
			// 此时应该遍历查找
			// 因为space/{alias}和space/test是两个不同的RequestMapping
			// 但是space/{alias}可以映射到space/test
			String lookupPath = "/" + path;
			for (Map.Entry<RequestMappingInfo, HandlerMethod> it : requestMappingHandlerMapping
					.getHandlerMethodsUnsafe().entrySet()) {
				HandlerMethod method = it.getValue();
				if (method.getBean() instanceof TemplateController) {
					PatternsRequestCondition condition = it.getKey().getPatternsCondition();
					if (!condition.getMatchingPatterns(lookupPath).isEmpty()) {
						return;
					}
				}
			}
			RequestMappingInfo info = getMethodMapping(path);
			requestMappingHandlerMapping.registerMappingUnsafe(info, new PreviewTemplateController(path), method);
		}

		/**
		 * 注册模板mapping
		 * 
		 * @param templateName
		 *            模板名
		 * @param path
		 *            路径
		 * @throws LogicException
		 *             路径已经存在并且无法清除
		 */
		public void registerTemplateMapping(String templateName, String path) throws LogicException {
			path = FileUtils.cleanPath(path);
			// 判断是否是系统保留路径
			if (isKeyPath(path)) {
				throw new LogicException("requestMapping.register.keyPath", "路径" + path + "是系统保留路径，无法被注册");
			}
			boolean exists = false;
			RequestMappingInfo info = getMethodMapping(path);
			HandlerMethod handlerMethod = requestMappingHandlerMapping.getHandlerMethodsUnsafe().get(info);
			if (handlerMethod != null) {
				exists = true;
				TemplateController templateController = (TemplateController) handlerMethod.getBean();
				// 如果是系统模板或者预览模板，删除后注册
				if (SystemTemplate.isSystemTemplate(templateController.templateName)
						|| Template.isPreviewTemplate(templateController.templateName)) {
					requestMappingHandlerMapping.unregisterMappingUnsafe(info);
					exists = false;
				}
			}
			if (exists) {
				throw new LogicException(new Message("requestMapping.register.exists", "路径:" + path + "已经存在", path));
			}
			requestMappingHandlerMapping.registerMappingUnsafe(info, new TemplateController(templateName, path),
					method);
		}

		/**
		 * 解除模板mapping
		 * 
		 * @param path
		 *            模板访问路径
		 */
		public void unregisterTemplateMapping(String path) {
			path = FileUtils.cleanPath(path);
			if (isKeyPath(path)) {
				return;
			}
			SystemTemplate template = defaultTemplates.get(path);
			requestMappingHandlerMapping.unregisterMappingUnsafe(getMethodMapping(path));
			// 插入默认系统模板
			if (template != null) {
				String templateName = template.getTemplateName();
				requestMappingHandlerMapping.registerMappingUnsafe(getMethodMapping(template.getPath()),
						new TemplateController(templateName, path), method);
			}
		}

		/**
		 * 强制注册模板mapping(先删除后注册)
		 * 
		 * @param templateName
		 *            模板名
		 * @param path
		 *            模板访问路径
		 */
		public void forceRegisterTemplateMapping(String templateName, String path) {
			path = FileUtils.cleanPath(path);
			if (isKeyPath(path)) {
				return;
			}
			RequestMappingInfo info = getMethodMapping(path);
			requestMappingHandlerMapping.unregisterMappingUnsafe(info);
			requestMappingHandlerMapping.registerMappingUnsafe(info, new TemplateController(templateName, path),
					method);
		}

		private RequestMappingInfo getMethodMapping(String registPath) {
			PatternsRequestCondition prc = new PatternsRequestCondition(registPath);
			RequestMethodsRequestCondition rmrc = new RequestMethodsRequestCondition(RequestMethod.GET);
			return new RequestMappingInfo(prc, rmrc, null, null, null, null, null);
		}

		private boolean isKeyPath(String path) {
			String lookupPath = "/" + path;
			return !condition.getMatchingPatterns(lookupPath).isEmpty();
		}

		private class PreviewTemplateController extends TemplateController {

			public PreviewTemplateController(String path) {
				super(Template.TEMPLATE_PREVIEW_PREFIX + path, path);
			}

			public TemplateView handleRequest(HttpServletRequest request) {
				if (Environment.isLogin()) {
					return templateView;
				} else {
					throw new TemplateNotFoundException(templateName);
				}
			}
		}
	}

	public class TemplateController {

		protected final TemplateView templateView;
		protected final String templateName;
		protected final String path;
		protected final Integer id;

		public TemplateController(String templateName, String path) {
			super();
			this.templateView = new TemplateView(templateName);
			this.templateName = templateName;
			this.path = path;
			this.id = templateIncrementId.incrementAndGet();
		}

		public TemplateView handleRequest(HttpServletRequest request) {
			// 如果用户登录状态并且预览服务中存在这个模板，那么返回预览模板名
			if (Environment.isLogin()) {
				String path = FileUtils
						.cleanPath(request.getRequestURI().substring(request.getContextPath().length() + 1));
				String previewTemplateName = Template.TEMPLATE_PREVIEW_PREFIX + path;
				Optional<String> bestTemplateName = previewService.getBestMatchTemplateName(this.path,
						previewTemplateName);
				if (bestTemplateName.isPresent()) {
					return new TemplateView(bestTemplateName.get());
				}
			}
			return templateView;
		}

		public Integer getId() {
			return id;
		}
	}

	/**
	 * 用来在一个<b>事务</b>中使mapping和页面保持一致
	 * 
	 * @author mhlx
	 *
	 */
	private final class PageRequestMappingRegisterHelper {

		private List<Runnable> rollBackActions = new ArrayList<>();
		private long stamp;

		public PageRequestMappingRegisterHelper() {
			super();
			if (!TransactionSynchronizationManager.isSynchronizationActive()) {
				throw new SystemException(this.getClass().getName() + " 必须处于一个事务中");
			}
			// 锁住RequestMapping
			this.stamp = templateMappingRegister.lockWrite();

			Transactions.afterCompletion(i -> {
				try {
					if (i == TransactionSynchronization.STATUS_ROLLED_BACK) {
						rollback();
					}
				} finally {
					templateMappingRegister.unlockWrite(stamp);
				}
			});
		}

		void registerPage(Page page) throws LogicException {
			String path = page.getTemplatePath();
			templateMappingRegister.registerTemplateMapping(page.getTemplateName(), path);
			rollBackActions.add(() -> {
				templateMappingRegister.unregisterTemplateMapping(path);
			});
		}

		void unregisterPage(Page page) {
			String path = page.getTemplatePath();
			templateMappingRegister.unregisterTemplateMapping(path);
			rollBackActions.add(() -> {
				templateMappingRegister.forceRegisterTemplateMapping(page.getTemplateName(), path);
			});
		}

		private void rollback() {
			if (!rollBackActions.isEmpty()) {
				for (Runnable act : rollBackActions) {
					act.run();
				}
			}
		}
	}

	private static final class SystemTemplate implements Template {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private static final String SYSTEM_PREFIX = TEMPLATE_PREFIX + "System" + SPLITER;
		private final String path;
		private String template;
		private String templateName;

		private SystemTemplate(SystemTemplate systemTemplate) {
			this.path = systemTemplate.path;
			this.template = systemTemplate.template;
		}

		public SystemTemplate(String path, String templateClassPath) {
			super();
			this.path = path;
			this.template = readClassPathResourceToString(templateClassPath);
		}

		@Override
		public boolean isRoot() {
			return true;
		}

		@Override
		public String getTemplate() throws IOException {
			return template;
		}

		@Override
		public String getTemplateName() {
			if (templateName == null) {
				templateName = SYSTEM_PREFIX + FileUtils.cleanPath(path);
			}
			return templateName;
		}

		@Override
		public Template cloneTemplate() {
			return new SystemTemplate(this);
		}

		@Override
		public boolean isCallable() {
			return false;
		}

		@Override
		public boolean equalsTo(Template other) {
			if (Validators.baseEquals(this, other)) {
				SystemTemplate rhs = (SystemTemplate) other;
				return Objects.equals(this.path, rhs.path);
			}
			return false;
		}

		public String getPath() {
			return path;
		}

		@Override
		public void clearTemplate() {
			this.template = null;
		}

		public static boolean isSystemTemplate(String templateName) {
			return templateName != null && templateName.startsWith(SYSTEM_PREFIX);
		}
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

	public void setPathTemplateRoot(String pathTemplateRoot) {
		this.pathTemplateRoot = Paths.get(pathTemplateRoot);
	}
}
