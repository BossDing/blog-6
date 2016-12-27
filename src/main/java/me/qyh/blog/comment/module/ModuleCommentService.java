package me.qyh.blog.comment.module;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import me.qyh.blog.comment.base.CommentConfig;
import me.qyh.blog.comment.base.CommentConfig.CommentMode;
import me.qyh.blog.comment.base.CommentSupport;
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.exception.SystemException;
import me.qyh.blog.util.UrlUtils;
import me.qyh.blog.util.Validators;

@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
public class ModuleCommentService extends CommentSupport<ModuleComment, ModuleCommentDao> {

	private static final String MODULE_PATTERN = "^[A-Za-z0-9]{0,20}$";
	private Map<String, CommentModule> moduleMap = Maps.newHashMap();
	private Map<Integer, CommentModule> idModuleMap = Maps.newHashMap();
	/**
	 * 评论配置文件位置
	 */
	private static final Resource configResource = new ClassPathResource("resources/moduleCommentConfig.properties");
	private final Properties pros = new Properties();
	private Map<CommentModule, CommentConfig> configMap = Maps.newHashMap();
	private CommentConfig defaultConfig;

	@Transactional(readOnly = true)
	public CommentPageResult<ModuleComment> queryComment(ModuleCommentQueryParam param) {
		param.setPageSize(defaultConfig.getPageSize());
		if (param.getModule() == null || !moduleMap.containsKey(param.getModule().getName())) {
			return new CommentPageResult<>(param, 0, Collections.emptyList(), new CommentConfig(defaultConfig));
		}
		CommentModule module = moduleMap.get(param.getModule().getName());
		CommentConfig config = loadCommentConfig(module);
		param.setPageSize(config.getPageSize());
		param.setModule(module);
		return super.queryComment(param, new CommentConfig(config));
	}

	public ModuleComment insertComment(ModuleComment comment) throws LogicException {
		CommentModule module = moduleMap.get(comment.getModule().getName());
		if (module == null) {
			throw new LogicException("comment.module.notexists", "评论模块不存在");
		}
		comment.setModule(module);
		super.insertComment(comment, loadCommentConfig(module));
		comment.setModule(module);
		sendEmail(comment);
		return comment;
	}

	@Transactional
	public List<ModuleComment> queryLastComments(String module, int limit, boolean queryAdmin) {
		List<ModuleComment> comments = commentDao.selectLastComments(moduleMap.get(module), limit, queryAdmin);
		for (ModuleComment comment : comments) {
			completeComment(comment);
		}
		return comments;
	}

	/**
	 * 审核评论
	 * 
	 * @param id
	 *            评论id
	 * @throws LogicException
	 */
	public void checkComment(Integer id) throws LogicException {
		super.checkComment(id);
	}

	/**
	 * 删除评论以及该评论下的所有子评论
	 * 
	 * @param id
	 *            评论id
	 * @throws LogicException
	 */
	public void deleteComment(Integer id) throws LogicException {
		super.deleteComment(id);
	}

	/**
	 * 查询会话
	 * 
	 * @param module
	 *            模块
	 * @param id
	 *            当前评论id
	 * @return
	 * @throws LogicException
	 */
	@Transactional(readOnly = true)
	public List<ModuleComment> queryConversations(String module, Integer id) throws LogicException {
		if (module == null || !moduleMap.containsKey(module)) {
			throw new LogicException("comment.module.notexists", "评论模块不存在");
		}
		ModuleComment comment = commentDao.selectById(id);
		if (comment == null) {
			throw new LogicException("comment.notExists", "评论不存在");
		}
		if (!comment.getModule().equals(moduleMap.get(module))) {
			throw new LogicException("comment.module.unmatch", "评论模块不匹配");
		}
		if (comment.getParents().isEmpty()) {
			return Arrays.asList(comment);
		}
		List<ModuleComment> comments = Lists.newArrayList();
		for (Integer pid : comment.getParents()) {
			ModuleComment p = commentDao.selectById(pid);
			completeComment(p);
			comments.add(p);
		}
		completeComment(comment);
		comments.add(comment);
		return comments;
	}

	public CommentConfig getCommentConfig(CommentModule module) {
		return module == null ? defaultConfig : loadCommentConfig(module);
	}

	public void setModules(List<CommentModule> modules) {
		for (CommentModule module : modules) {
			if (Validators.isEmptyOrNull(module.getName(), true)) {
				throw new SystemException("评论模块名不能为空");
			}
			if (!module.getName().matches(MODULE_PATTERN)) {
				throw new SystemException("评论模块只能为大小写英文和数字，长度为0~20个字符");
			}
			if (module.getId() == null) {
				throw new SystemException("评论模块id不能为空");
			}
			String url = module.getUrl();
			if (Validators.isEmptyOrNull(url, true)) {
				throw new SystemException("评论链接不能为空");
			}
			url = url.trim();
			if (!UrlUtils.isAbsoluteUrl(url)) {
				String prefix = urlHelper.getUrl();
				url = url.startsWith("/") ? prefix + url : prefix + '/' + url;
			}
			module.setUrl(url);
			if (this.idModuleMap.containsKey(module.getId())) {
				throw new SystemException("已经存在id为" + module.getId() + "评论模块");
			}
			if (this.moduleMap.containsKey(module.getName())) {
				throw new SystemException("已经存在名为" + module.getName() + "评论模块");
			}
			this.idModuleMap.put(module.getId(), module);
			this.moduleMap.put(module.getName(), module);
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {

		super.afterPropertiesSet();
		// 读取配置文件内容
		try (InputStream is = configResource.getInputStream()) {
			pros.load(is);
		}

		defaultConfig = loadCommentConfig("");
	}

	@Override
	protected void completeComment(ModuleComment comment) {
		super.completeComment(comment);
		if (comment.getModule() != null) {
			CommentModule module = idModuleMap.get(comment.getModule().getId());
			if (module != null) {
				comment.setModule(module);
				comment.setUrl(module.getUrl());
			}
		}
	}

	private CommentConfig loadCommentConfig(CommentModule module) {
		CommentConfig config = configMap.get(module);
		if (config == null) {
			synchronized (this) {
				if (config == null) {
					String prefix = module.getName() + ".";
					config = loadCommentConfig(prefix);
				}
			}
		}
		return config;
	}

	private CommentConfig loadCommentConfig(String prefix) {
		CommentConfig config = new CommentConfig();
		config.setAllowHtml(Boolean.parseBoolean(pros.getProperty(prefix + COMMENT_ALLOW_HTML, "false")));
		config.setAsc(Boolean.parseBoolean(pros.getProperty(prefix + COMMENT_ASC, "true")));
		config.setCheck(Boolean.parseBoolean(pros.getProperty(prefix + COMMENT_CHECK, "false")));
		String commentMode = pros.getProperty(prefix + COMMENT_MODE);
		if (commentMode == null) {
			config.setCommentMode(CommentMode.LIST);
		} else {
			config.setCommentMode(CommentMode.valueOf(commentMode));
		}
		config.setLimitCount(Integer.parseInt(pros.getProperty(prefix + COMMENT_LIMIT_COUNT, "10")));
		config.setLimitSec(Integer.parseInt(pros.getProperty(prefix + COMMENT_LIMIT_SEC, "60")));
		config.setPageSize(Integer.parseInt(pros.getProperty(prefix + COMMENT_PAGESIZE, "10")));
		return config;
	}

}
