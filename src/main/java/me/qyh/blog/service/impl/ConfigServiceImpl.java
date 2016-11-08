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

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import me.qyh.blog.config.PageSizeConfig;
import me.qyh.blog.config.UploadConfig;
import me.qyh.blog.entity.CommentConfig;
import me.qyh.blog.entity.CommentConfig.CommentMode;
import me.qyh.blog.exception.LogicException;
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
		config.setUserFragmentPageSize(getInt(PAGE_SIZE_USERFRAGEMENT, 5));
		config.setOauthUserPageSize(getInt(PAGE_SIZE_OAUTHUSER, 5));
		config.setCommentPageSize(getInt(PAGE_SIZE_COMMENT, 5));
		return config;
	}

	@Override
	@Cacheable(key = "'commentConfig'", value = "configCache", unless = "#result == null")
	public CommentConfig getCommentConfig() {
		CommentConfig config = new CommentConfig();
		config.setAllowComment(getBoolean(ALLOW_COMMENT, true));
		config.setAllowHtml(getBoolean(COMMENT_ALLOW_HTML, false));
		config.setAsc(getBoolean(COMMENT_ASC, true));
		config.setCheck(getBoolean(COMMENT_CHECK, false));
		String commentMode = this.config.getProperty(COMMENT_MODE);
		if (commentMode == null)
			config.setCommentMode(CommentMode.LIST);
		else
			config.setCommentMode(CommentMode.valueOf(commentMode));
		config.setLimitCount(getInt(COMMENT_LIMIT_COUNT, 10));
		config.setLimitSec(getInt(COMMENT_LIMIT_SEC, 60));
		return config;
	}

	@Override
	@CachePut(key = "'commentConfig'", value = "configCache")
	public CommentConfig updateCommentConfig(CommentConfig commentConfig) {
		config.setProperty(COMMENT_ALLOW_HTML, commentConfig.getAllowHtml().toString());
		config.setProperty(COMMENT_ASC, commentConfig.getAsc().toString());
		config.setProperty(COMMENT_CHECK, commentConfig.getCheck().toString());
		config.setProperty(COMMENT_LIMIT_COUNT, commentConfig.getLimitCount().toString());
		config.setProperty(COMMENT_LIMIT_SEC, commentConfig.getLimitSec().toString());
		config.setProperty(COMMENT_MODE, commentConfig.getCommentMode().name());
		config.setProperty(ALLOW_COMMENT, commentConfig.getAllowComment().toString());
		store();
		return commentConfig;
	}

	@Override
	@CachePut(key = "'pageSizeConfig'", value = "configCache")
	public PageSizeConfig updatePageSizeConfig(PageSizeConfig pageSizeConfig) {
		config.setProperty(PAGE_SIZE_FILE, pageSizeConfig.getFilePageSize() + "");
		config.setProperty(PAGE_SIZE_TAG, pageSizeConfig.getTagPageSize() + "");
		config.setProperty(PAGE_SIZE_ARICLE, pageSizeConfig.getArticlePageSize() + "");
		config.setProperty(PAGE_SIZE_USERFRAGEMENT, pageSizeConfig.getUserFragmentPageSize() + "");
		config.setProperty(PAGE_SIZE_USERPAGE, pageSizeConfig.getUserPagePageSize() + "");
		config.setProperty(PAGE_SIZE_OAUTHUSER, pageSizeConfig.getOauthUserPageSize() + "");
		config.setProperty(PAGE_SIZE_COMMENT, pageSizeConfig.getCommentPageSize() + "");
		store();
		return pageSizeConfig;
	}

	@Override
	@Cacheable(key = "'mateweblogUploadConfig'", value = "configCache", unless = "#result == null")
	public UploadConfig getMetaweblogConfig() {
		UploadConfig uploadConfig = new UploadConfig();
		String path = config.getProperty(METAWEBLOG_UPLOAD_PATH);
		if (path == null)
			uploadConfig.setPath("");
		else
			uploadConfig.setPath(path);
		uploadConfig.setServer(getInt(METAWEBLOG_UPLOAD_SERVER, null));
		return uploadConfig;
	}

	@Override
	@CachePut(key = "'mateweblogUploadConfig'", value = "configCache")
	public UploadConfig updateMetaweblogConfig(UploadConfig uploadConfig) throws LogicException {
		if (uploadConfig.getPath() != null)
			config.setProperty(METAWEBLOG_UPLOAD_PATH, uploadConfig.getPath());
		if (uploadConfig.getServer() != null)
			config.setProperty(METAWEBLOG_UPLOAD_SERVER, uploadConfig.getServer().toString());
		store();
		return uploadConfig;
	}

	private Integer getInt(String key, Integer _default) {
		if (config.containsKey(key)) {
			return Integer.parseInt(config.getProperty(key));
		}
		return _default;
	}

	private Boolean getBoolean(String key, boolean _default) {
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
	private static final String PAGE_SIZE_USERFRAGEMENT = "pagesize.userfragment";
	private static final String PAGE_SIZE_USERPAGE = "pagesize.userpage";
	private static final String PAGE_SIZE_ARICLE = "pagesize.article";
	private static final String PAGE_SIZE_TAG = "pagesize.tag";
	private static final String PAGE_SIZE_OAUTHUSER = "pagesize.oauthuser";
	private static final String PAGE_SIZE_COMMENT = "pagesize.comment";

	private static final String ALLOW_COMMENT = "commentConfig.allowComment";
	private static final String COMMENT_MODE = "commentConfig.commentMode";
	private static final String COMMENT_ASC = "commentConfig.commentAsc";
	private static final String COMMENT_ALLOW_HTML = "commentConfig.commentAllowHtml";
	private static final String COMMENT_LIMIT_SEC = "commentConfig.commentLimitSec";
	private static final String COMMENT_LIMIT_COUNT = "commentConfig.commentLimitCount";
	private static final String COMMENT_CHECK = "commentConfig.commentCheck";

	private static final String METAWEBLOG_UPLOAD_PATH = "metaweblog.upload.path";
	private static final String METAWEBLOG_UPLOAD_SERVER = "metaweblog.upload.server";

}
