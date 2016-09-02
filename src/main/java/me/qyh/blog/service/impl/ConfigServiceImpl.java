package me.qyh.blog.service.impl;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import me.qyh.blog.config.CommentConfig;
import me.qyh.blog.config.Limit;
import me.qyh.blog.config.PageSizeConfig;
import me.qyh.blog.exception.SystemException;
import me.qyh.blog.service.ConfigService;

@Service
public class ConfigServiceImpl implements ConfigService, InitializingBean {

	private Properties config = new Properties();
	private Resource resource;

	@Override
	@Cacheable(key = "'pageSizeConfig'", value = "configCache", unless = "#result == null")
	public PageSizeConfig getPageSizeConfig() {
		PageSizeConfig config = new PageSizeConfig();
		config.setArticlePageSize(getInt(PAGE_SIZE_ARICLE, 5));
		config.setFilePageSize(getInt(PAGE_SIZE_FILE, 5));
		config.setTagPageSize(getInt(PAGE_SIZE_TAG, 5));
		config.setUserPagePageSize(getInt(PAGE_SIZE_USERPAGE, 5));
		config.setUserWidgetPageSize(getInt(PAGE_SIZE_USERWIDGET, 5));
		config.setOauthUserPageSize(getInt(PAGE_SIZE_OAUTHUSER, 5));
		config.setCommentPageSize(getInt(PAGE_SIZE_COMMENT, 5));
		return config;
	}

	@Override
	@CachePut(key = "'pageSizeConfig'", value = "configCache")
	public PageSizeConfig updatePageSizeConfig(PageSizeConfig pageSizeConfig) {
		config.setProperty(PAGE_SIZE_FILE, pageSizeConfig.getFilePageSize() + "");
		config.setProperty(PAGE_SIZE_TAG, pageSizeConfig.getTagPageSize() + "");
		config.setProperty(PAGE_SIZE_ARICLE, pageSizeConfig.getArticlePageSize() + "");
		config.setProperty(PAGE_SIZE_USERWIDGET, pageSizeConfig.getUserWidgetPageSize() + "");
		config.setProperty(PAGE_SIZE_USERPAGE, pageSizeConfig.getUserPagePageSize() + "");
		config.setProperty(PAGE_SIZE_OAUTHUSER, pageSizeConfig.getOauthUserPageSize() + "");
		config.setProperty(PAGE_SIZE_COMMENT, pageSizeConfig.getCommentPageSize() + "");
		store();
		return pageSizeConfig;
	}

	@Override
	@Cacheable(key = "'CommentConfig'", value = "configCache", unless = "#result == null")
	public CommentConfig getCommentConfig() {
		CommentConfig config = new CommentConfig();
		config.setAsc(getBoolean(COMMENT_SORT, true));
		Limit limit = new Limit();
		limit.setLimit(getInt(COMMENT_LIMIT, 3));
		limit.setTime(getLong(COMMENT_LIMIT_SECOND, 10));
		limit.setUnit(TimeUnit.SECONDS);
		config.setLimit(limit);
		return config;
	}

	@Override
	@CachePut(key = "'CommentConfig'", value = "configCache")
	public CommentConfig updateCommentConfig(CommentConfig commentConfig) {
		config.setProperty(COMMENT_SORT, commentConfig.isAsc() + "");
		config.setProperty(COMMENT_LIMIT, commentConfig.getLimit().getLimit() + "");
		config.setProperty(COMMENT_LIMIT_SECOND, commentConfig.getLimit().getTime() + "");
		store();
		return commentConfig;
	}

	private Integer getInt(String key, int _default) {
		if (config.containsKey(key)) {
			return Integer.parseInt(config.getProperty(key));
		}
		return _default;
	}

	private long getLong(String key, long _default) {
		if (config.containsKey(key)) {
			return Long.parseLong(config.getProperty(key));
		}
		return _default;
	}

	private boolean getBoolean(String key, boolean _default) {
		if (config.containsKey(key)) {
			return Boolean.parseBoolean(config.getProperty(key));
		}
		return _default;
	}

	private void store() {
		OutputStream os = null;
		try {
			os = new FileOutputStream(resource.getFile());
			config.store(os, "");
		} catch (Exception e) {
			throw new SystemException(e.getMessage(), e);
		} finally {
			IOUtils.closeQuietly(os);
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		// 加载config.properties
		resource = new ClassPathResource("resources/config.properties");
		config.load(resource.getInputStream());
	}

	private static final String PAGE_SIZE_FILE = "pagesize.file";
	private static final String PAGE_SIZE_USERWIDGET = "pagesize.userwidget";
	private static final String PAGE_SIZE_USERPAGE = "pagesize.userpage";
	private static final String PAGE_SIZE_ARICLE = "pagesize.article";
	private static final String PAGE_SIZE_TAG = "pagesize.tag";
	private static final String PAGE_SIZE_OAUTHUSER = "pagesize.oauthuser";
	private static final String PAGE_SIZE_COMMENT = "pagesize.comment";

	private static final String COMMENT_SORT = "comment.sort";
	private static final String COMMENT_LIMIT = "comment.limit";
	private static final String COMMENT_LIMIT_SECOND = "comment.limit.second";
}
