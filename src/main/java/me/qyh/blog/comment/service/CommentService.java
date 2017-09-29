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
package me.qyh.blog.comment.service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Properties;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import me.qyh.blog.comment.dao.CommentDao;
import me.qyh.blog.comment.entity.Comment;
import me.qyh.blog.comment.entity.Comment.CommentStatus;
import me.qyh.blog.comment.entity.CommentMode;
import me.qyh.blog.comment.entity.CommentModule;
import me.qyh.blog.comment.event.CommentEvent;
import me.qyh.blog.comment.module.CommentModuleHandler;
import me.qyh.blog.comment.vo.CommentPageResult;
import me.qyh.blog.comment.vo.CommentQueryParam;
import me.qyh.blog.comment.vo.CommentStatistics;
import me.qyh.blog.core.config.Constants;
import me.qyh.blog.core.context.Environment;
import me.qyh.blog.core.entity.Editor;
import me.qyh.blog.core.entity.Space;
import me.qyh.blog.core.entity.User;
import me.qyh.blog.core.exception.LogicException;
import me.qyh.blog.core.exception.SystemException;
import me.qyh.blog.core.service.CommentServer;
import me.qyh.blog.core.service.UserService;
import me.qyh.blog.core.text.HtmlClean;
import me.qyh.blog.core.text.Markdown2Html;
import me.qyh.blog.core.util.FileUtils;
import me.qyh.blog.core.util.Resources;
import me.qyh.blog.core.vo.Limit;
import me.qyh.blog.core.vo.PageQueryParam;
import me.qyh.blog.core.vo.PageResult;

@Service
public class CommentService implements InitializingBean, CommentServer, ApplicationEventPublisherAware {

	@Autowired(required = false)
	private CommentChecker commentChecker;
	@Autowired
	private HtmlClean htmlClean;
	@Autowired
	protected CommentDao commentDao;
	@Autowired
	private Markdown2Html markdown2Html;
	@Autowired
	private UserService userService;

	private ApplicationEventPublisher applicationEventPublisher;

	/**
	 * 评论配置项
	 */
	protected static final String COMMENT_MODE = "commentConfig.commentMode";
	protected static final String COMMENT_ASC = "commentConfig.commentAsc";
	protected static final String COMMENT_EDITOR = "commentConfig.editor";
	protected static final String COMMENT_LIMIT_SEC = "commentConfig.commentLimitSec";
	protected static final String COMMENT_LIMIT_COUNT = "commentConfig.commentLimitCount";
	protected static final String COMMENT_CHECK = "commentConfig.commentCheck";
	protected static final String COMMENT_PAGESIZE = "commentConfig.commentPageSize";

	private final Comparator<Comment> ascCommentComparator = Comparator.comparing(Comment::getCommentDate)
			.thenComparing(Comment::getId);
	private final Comparator<Comment> descCommentComparator = (t1, t2) -> -ascCommentComparator.compare(t1, t2);

	/**
	 * 为了保证一个树结构，这里采用 path来纪录层次结构
	 * {@link http://stackoverflow.com/questions/4057947/multi-tiered-comment-replies-display-and-storage}.
	 * 同时为了走索引，只能限制它为255个字符，由于id为数字的原因，实际上一般情况下很难达到255的长度(即便id很大)，所以这里完全够用
	 */
	private static final int PATH_MAX_LENGTH = 255;

	/**
	 * 评论配置文件位置
	 */

	private static final Path RES_PATH = Constants.CONFIG_DIR.resolve("commentConfig.properties");
	private final Resource configResource = new PathResource(RES_PATH);
	private final Properties pros = new Properties();

	private CommentConfig config;

	private Map<String, CommentModuleHandler> handlerMap = new HashMap<>();

	static {
		FileUtils.createFile(RES_PATH);
	}

	@Override
	public void afterPropertiesSet() throws Exception {

		if (commentChecker == null) {
			commentChecker = (comment, config) -> {
			};
		}

		Resources.readResource(configResource, pros::load);
		loadConfig();
	}

	/**
	 * 审核评论
	 * 
	 * @param id
	 *            评论id
	 * @return
	 * @throws LogicException
	 */
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	public Comment checkComment(Integer id) throws LogicException {
		Comment comment = commentDao.selectById(id);// 查询父评论
		if (comment == null) {
			throw new LogicException("comment.notExists", "评论不存在");
		}
		if (!comment.isChecking()) {
			throw new LogicException("comment.checked", "评论审核过了");
		}
		commentDao.updateStatusToNormal(comment);

		return comment;
	}

	/**
	 * 删除评论
	 * 
	 * @param id
	 *            评论id
	 * @throws LogicException
	 */
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	public void deleteComment(Integer id) throws LogicException {
		Comment comment = commentDao.selectById(id);
		if (comment == null) {
			throw new LogicException("comment.notExists", "评论不存在");
		}
		commentDao.deleteByPath(comment.getParentPath() + comment.getId());
		commentDao.deleteById(id);
	}

	/**
	 * 更新评论配置
	 * 
	 * @param config
	 *            配置
	 */
	public synchronized CommentConfig updateCommentConfig(CommentConfig config) {
		pros.setProperty(COMMENT_EDITOR, config.getEditor().name());
		pros.setProperty(COMMENT_CHECK, config.getCheck().toString());
		pros.setProperty(COMMENT_LIMIT_COUNT, config.getLimitCount().toString());
		pros.setProperty(COMMENT_LIMIT_SEC, config.getLimitSec().toString());
		pros.setProperty(COMMENT_PAGESIZE, config.getPageSize() + "");
		try (OutputStream os = new FileOutputStream(configResource.getFile())) {
			pros.store(os, "");
		} catch (IOException e) {
			throw new SystemException(e.getMessage(), e);
		}
		loadConfig();
		return config;
	}

	/**
	 * 获取评论配置
	 * 
	 * @return
	 */
	public CommentConfig getCommentConfig() {
		return new CommentConfig(config);
	}

	/**
	 * 新增一条评论
	 * 
	 * @param comment
	 * @return
	 * @throws LogicException
	 */
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	public Comment insertComment(Comment comment) throws LogicException {
		CommentModule module = comment.getCommentModule();

		CommentModuleHandler handler = handlerMap.get(module.getModule());
		if (handler == null) {
			throw new LogicException("comment.module.invalid", "评论模块不存在");
		}
		handler.doValidateBeforeInsert(module.getId());

		long now = System.currentTimeMillis();
		String ip = comment.getIp();
		if (!Environment.isLogin()) {
			// 检查频率
			Limit limit = config.getLimit();
			long start = now - limit.getUnit().toMillis(limit.getTime());
			int count = commentDao.selectCountByIpAndDatePeriod(comment.getCommentModule(), new Timestamp(start),
					new Timestamp(now), ip) + 1;
			if (count > limit.getCount()) {
				throw new LogicException("comment.overlimit", "评论太过频繁，请稍作休息");
			}
		}

		commentChecker.checkComment(comment, config);

		String parentPath = "/";
		// 判断是否存在父评论
		Comment parent = comment.getParent();
		if (parent != null) {
			parent = commentDao.selectById(parent.getId());// 查询父评论
			if (parent == null) {
				throw new LogicException("comment.parent.notExists", "父评论不存在");
			}

			// 如果父评论正在审核
			if (parent.isChecking()) {
				throw new LogicException("comment.parent.checking", "父评论正在审核");
			}

			if (!comment.matchParent(parent)) {
				throw new LogicException("comment.parent.unmatch", "评论匹配失败");
			}
			parentPath = parent.getParentPath() + parent.getId() + "/";
		}
		if (parentPath.length() > PATH_MAX_LENGTH) {
			throw new LogicException("comment.path.toolong", "该评论不能再被回复了");
		}

		Comment last = commentDao.selectLast(comment);
		if (last != null && last.getContent().equals(comment.getContent())) {
			throw new LogicException("comment.content.same", "已经回复过相同的评论了");
		}

		if (!Environment.isLogin()) {
			String email = comment.getEmail();
			if (email != null) {
				// set gravatar md5
				comment.setGravatar(DigestUtils.md5DigestAsHex(email.getBytes(Constants.CHARSET)));
			}
			comment.setAdmin(false);

		} else {
			// 管理员回复无需设置评论用户信息
			comment.setEmail(null);
			comment.setNickname(null);
			comment.setAdmin(true);
			comment.setWebsite(null);
		}

		comment.setParentPath(parentPath);
		comment.setCommentDate(new Timestamp(now));

		boolean check = config.getCheck() && !Environment.isLogin();
		comment.setStatus(check ? CommentStatus.CHECK : CommentStatus.NORMAL);
		// 获取当前设置的编辑器
		comment.setEditor(config.getEditor());
		comment.setParent(parent);

		commentDao.insert(comment);

		completeComment(comment);

		applicationEventPublisher.publishEvent(new CommentEvent(this, comment));

		return comment;
	}

	/**
	 * 分页查询评论
	 * 
	 * @param param
	 * @return
	 */
	@Transactional(readOnly = true)
	public CommentPageResult queryComment(CommentQueryParam param) {
		param.setPageSize(Math.min(config.getPageSize(), param.getPageSize()));
		if (!param.complete()) {
			return new CommentPageResult(param, 0, Collections.emptyList(), new CommentConfig(config));
		}
		CommentModule module = param.getModule();
		CommentModuleHandler handler = handlerMap.get(module.getModule());
		if (handler == null || !handler.doValidateBeforeQuery(module.getId())) {
			return new CommentPageResult(param, 0, Collections.emptyList(), new CommentConfig(config));
		}

		CommentMode mode = param.getMode();
		int count;
		switch (mode) {
		case TREE:
			count = commentDao.selectCountWithTree(param);
			break;
		default:
			count = commentDao.selectCountWithList(param);
			break;
		}
		int pageSize = config.getPageSize();
		if (count == 0) {
			return new CommentPageResult(param, 0, Collections.emptyList(), new CommentConfig(config));
		}
		boolean asc = param.isAsc();
		if (param.getCurrentPage() <= 0) {
			if (asc) {
				param.setCurrentPage(count % pageSize == 0 ? count / pageSize : count / pageSize + 1);
			} else {
				param.setCurrentPage(1);
			}
		}
		List<Comment> datas;
		switch (mode) {
		case TREE:
			datas = commentDao.selectPageWithTree(param);
			for (Comment comment : datas) {
				completeComment(comment);
			}
			datas = handleTree(datas, param.isAsc());
			break;
		default:
			datas = commentDao.selectPageWithList(param);
			for (Comment comment : datas) {
				completeComment(comment);
			}
			break;
		}
		return new CommentPageResult(param, count, datas, new CommentConfig(config));
	}

	/**
	 * 删除评论
	 * 
	 * @param ip
	 * @param referenceId
	 * @throws LogicException
	 */
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	public void deleteComment(CommentModule module) {
		commentDao.deleteByModule(module);
	}

	/**
	 * 查询<b>当前空间</b>下 某个模块类型的最近的评论
	 * <p>
	 * <b>用于DataTag</b>
	 * </p>
	 * 
	 * @param type
	 *            模块类型
	 * @param limit
	 *            数目限制
	 * @param queryAdmin
	 *            是否包含管理员
	 * @return
	 */
	@Transactional(readOnly = true)
	public List<Comment> queryLastComments(String module, int limit, boolean queryAdmin) {
		if (StringUtils.isEmpty(module)) {
			return Collections.emptyList();
		}
		CommentModuleHandler handler = handlerMap.get(module);
		if (handler == null) {
			return Collections.emptyList();
		}
		List<Comment> comments = handler.queryLastComments(Environment.getSpace(), limit, Environment.isLogin(),
				queryAdmin);
		for (Comment comment : comments) {
			completeComment(comment);
		}
		return comments;
	}

	/**
	 * 查询会话
	 * 
	 * @return
	 * @throws LogicException
	 */
	@Transactional(readOnly = true)
	public List<Comment> queryConversations(CommentModule module, Integer id) throws LogicException {
		if (module.getModule() == null || module.getId() == null) {
			return Collections.emptyList();
		}
		CommentModuleHandler handler = handlerMap.get(module.getModule());
		if (handler == null || !handler.doValidateBeforeQuery(module.getId())) {
			return Collections.emptyList();
		}
		Comment comment = commentDao.selectById(id);
		if (comment == null) {
			throw new LogicException("comment.notExists", "评论不存在");
		}
		if (!comment.getCommentModule().equals(module)) {
			return Collections.emptyList();
		}
		completeComment(comment);
		if (comment.getParents().isEmpty()) {
			return Arrays.asList(comment);
		}
		List<Comment> comments = new ArrayList<>();
		for (Integer pid : comment.getParents()) {
			Comment p = commentDao.selectById(pid);
			completeComment(p);
			comments.add(p);
		}
		comments.add(comment);
		return comments;
	}

	/**
	 * 查询未审核评论的数目
	 * 
	 * @since 5.5.6
	 * @return
	 */
	@Transactional
	public int queryUncheckCommentCount() {
		return commentDao.queryUncheckCommentsCount();
	}

	/**
	 * 分页查询待审核评论
	 * 
	 * @param param
	 * @return
	 */
	@Transactional(readOnly = true)
	public PageResult<Comment> queryUncheckComments(PageQueryParam param) {
		param.setPageSize(Math.min(config.getPageSize(), param.getPageSize()));
		int count = commentDao.queryUncheckCommentsCount();
		List<Comment> comments = commentDao.queryUncheckComments(param);
		Map<String, List<CommentModule>> moduleMap = comments.stream().map(Comment::getCommentModule)
				.collect(Collectors.groupingBy(CommentModule::getModule));
		Map<CommentModule, Object> referenceMap = new HashMap<>();
		CommentModuleHandler handler;
		for (Map.Entry<String, List<CommentModule>> it : moduleMap.entrySet()) {
			String key = it.getKey();
			handler = handlerMap.get(key);
			if (handler == null) {
				throw new SystemException("无法找到CommentModuleHandler：" + key);
			}
			Map<Integer, Object> references;
			List<CommentModule> values = it.getValue();
			if (values.isEmpty()) {
				references = Collections.emptyMap();
			} else {
				references = handler
						.getReferences(values.stream().map(CommentModule::getId).collect(Collectors.toSet()));
			}
			for (Map.Entry<Integer, Object> refIt : references.entrySet()) {
				referenceMap.put(new CommentModule(key, refIt.getKey()), refIt.getValue());
			}
		}
		for (Comment comment : comments) {
			CommentModule module = comment.getCommentModule();
			module.setObject(referenceMap.get(module));
			completeComment(comment);
		}

		return new PageResult<>(param, count, comments);
	}

	@Override
	@Transactional(readOnly = true)
	public OptionalInt queryCommentNum(String module, Integer moduleId) {
		CommentModuleHandler handler = handlerMap.get(module);
		if (handler != null) {
			return handler.queryCommentNum(moduleId);
		}
		return OptionalInt.empty();
	}

	@Override
	@Transactional(readOnly = true)
	public Map<Integer, Integer> queryCommentNums(String module, Collection<Integer> moduleIds) {
		CommentModuleHandler handler = handlerMap.get(module);
		if (handler != null) {
			return handler.queryCommentNums(moduleIds);
		}
		return Collections.emptyMap();
	}

	@Override
	@Transactional(readOnly = true)
	public OptionalInt queryCommentNum(String module, Space space, boolean queryPrivate) {
		CommentModuleHandler handler = handlerMap.get(module);
		if (handler != null) {
			return OptionalInt.of(handler.queryCommentNum(space, queryPrivate));
		}
		return OptionalInt.empty();
	}

	/**
	 * 查询评论统计情况
	 * @param space 当前空间
	 * @return
	 */
	@Transactional(readOnly = true)
	public CommentStatistics queryCommentStatistics(Space space) {
		CommentStatistics commentStatistics = new CommentStatistics();
		boolean queryPrivate = Environment.isLogin();
		Map<String, Integer> map = new HashMap<>(handlerMap.size());
		for (CommentModuleHandler handler : handlerMap.values()) {
			map.put(handler.getType(), handler.queryCommentNum(space, queryPrivate));
		}
		commentStatistics.setStMap(map);
		return commentStatistics;
	}

	/**
	 * 查询某个评论模块项目的地址
	 * 
	 * @param module
	 * @return
	 */
	@Transactional(readOnly = true)
	public Optional<String> queryCommentModuleUrl(CommentModule module) {
		CommentModuleHandler handler = handlerMap.get(module.getModule());
		
		if(handler != null){
			return handler.getUrl(module.getId());
		}
		
		return Optional.empty();
	}

	private List<Comment> buildTree(List<Comment> comments) {
		CollectFilteredFilter filter = new CollectFilteredFilter(null);
		List<Comment> roots = new ArrayList<>();
		comments.stream().filter(filter).collect(Collectors.toList())
				.forEach(comment -> roots.add(pickByParent(comment, filter.rests)));
		return roots;
	}

	private Comment pickByParent(Comment parent, List<Comment> comments) {
		Objects.requireNonNull(parent);
		CollectFilteredFilter filter = new CollectFilteredFilter(parent);
		List<Comment> children = comments.stream().filter(filter).collect(Collectors.toList());
		children.forEach(child -> pickByParent(child, filter.rests));
		parent.setChildren(children);
		return parent;
	}

	private List<Comment> handleTree(List<Comment> comments, boolean asc) {
		if (comments.isEmpty()) {
			return comments;
		}
		List<Comment> tree = buildTree(comments);
		tree.sort(asc ? ascCommentComparator : descCommentComparator);
		return tree;
	}

	private void completeComment(Comment comment) {
		String content = comment.getContent();
		if (comment.getEditor().equals(Editor.MD)) {
			content = markdown2Html.toHtml(comment.getContent());
		}
		comment.setContent(htmlClean.clean(content));
		Comment p = comment.getParent();
		if (p != null) {
			fillComment(p);
		}
		fillComment(comment);
	}

	private void fillComment(Comment comment) {
		if (comment.getAdmin() == null || !comment.getAdmin()) {
			return;
		}
		User user = userService.getUser();
		comment.setNickname(user.getName());
		String email = user.getEmail();
		comment.setEmail(email);
		comment.setGravatar(user.getGravatar());
	}

	private void loadConfig() {
		config = new CommentConfig();
		config.setEditor(Editor.valueOf(pros.getProperty(COMMENT_EDITOR, "MD")));
		config.setCheck(Boolean.parseBoolean(pros.getProperty(COMMENT_CHECK, "false")));
		config.setLimitCount(Integer.parseInt(pros.getProperty(COMMENT_LIMIT_COUNT, "10")));
		config.setLimitSec(Integer.parseInt(pros.getProperty(COMMENT_LIMIT_SEC, "60")));
		config.setPageSize(Integer.parseInt(pros.getProperty(COMMENT_PAGESIZE, "10")));
	}

	private final class CollectFilteredFilter implements Predicate<Comment> {
		private final Comment parent;
		private List<Comment> rests = new ArrayList<>();

		public CollectFilteredFilter(Comment parent) {
			this.parent = parent;
		}

		@Override
		public boolean test(Comment t) {
			if (Objects.equals(parent, t.getParent())) {
				return true;
			}
			rests.add(t);
			return false;
		}
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		this.applicationEventPublisher = applicationEventPublisher;
	}

	/**
	 * 添加一个评论模块处理器
	 * 
	 * @param handler
	 */
	public void addCommentModuleHandler(CommentModuleHandler handler) {
		Objects.requireNonNull(handler);
		handlerMap.remove(handler.getType());
		handlerMap.put(handler.getType(), handler);
	}
}
